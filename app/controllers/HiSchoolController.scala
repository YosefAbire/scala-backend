package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import domain._
import repositories.{HiSchoolRepository, UserRepository}
import services.AiService
import auth.JwtService
import scala.concurrent.ExecutionContext

@Singleton
class HiSchoolController @Inject() (
    cc: ControllerComponents,
    hiSchoolRepository: HiSchoolRepository,
    userRepository: UserRepository,
    jwtService: JwtService,
    aiService: AiService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def extractToken(request: Request[?]): Option[String] = {
    request.cookies.get("access_token").map(_.value).orElse {
      request.cookies.get("hicenter_session").map(_.value).orElse {
        request.headers.get("Authorization").flatMap { auth =>
          if (auth.startsWith("Bearer ")) Some(auth.substring(7)) else None
        }
      }
    }
  }

  private def withUser(request: Request[?])(block: (Long, String) => Result): Result = {
    extractToken(request).flatMap(jwtService.validateToken) match {
      case Some(claim) => block(claim.userId, claim.role)
      case None => Unauthorized(Json.obj("detail" -> "Authentication required."))
    }
  }

  def listNotes: Action[AnyContent] = Action {
    val notes = hiSchoolRepository.allNotes()
    val dtos = notes.map { n =>
      Json.obj(
        "id" -> n.id,
        "title" -> n.title,
        "subject" -> n.subject,
        "chapter" -> n.chapter,
        "summary" -> n.summary,
        "is_verified" -> n.verified,
        "verified_by" -> n.verifiedByLabel,
        "verified_at" -> n.verifiedAt.map(_.toString),
        "verification_comment" -> n.verificationComment,
        "download_count" -> n.downloadCount,
        "author" -> "Faculty Chair",
        "created_at" -> n.createdAt.toString
      )
    }
    Ok(Json.toJson(dtos))
  }

  def getNote(id: Long): Action[AnyContent] = Action {
    hiSchoolRepository.findNoteById(id) match {
      case Some(n) =>
        hiSchoolRepository.incrementNoteDownload(id)
        Ok(Json.obj(
          "id" -> n.id,
          "title" -> n.title,
          "subject" -> n.subject,
          "chapter" -> n.chapter,
          "summary" -> n.summary,
          "is_verified" -> n.verified,
          "verified_by" -> n.verifiedByLabel,
          "verified_at" -> n.verifiedAt.map(_.toString),
          "verification_comment" -> n.verificationComment,
          "download_count" -> (n.downloadCount + 1),
          "author" -> "Faculty Chair",
          "created_at" -> n.createdAt.toString
        ))
      case None => NotFound(Json.obj("detail" -> "Note not found."))
    }
  }

  def createNote: Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { (userId, role) =>
      val title = (request.body \ "title").asOpt[String].getOrElse("New Study Note")
      val subject = (request.body \ "subject").asOpt[String].getOrElse("General")
      val summary = (request.body \ "summary").asOpt[String].getOrElse("Study note content summary.")

      val note = hiSchoolRepository.createNote(title, subject, summary, authorId = userId)

      // Asynchronous background RAG note vector indexing to hicenter-ai
      aiService.indexNoteChunk(
        noteId = note.id,
        schoolId = note.schoolId,
        title = note.title,
        subject = note.subject,
        chapter = note.chapter,
        content = note.summary
      )

      Ok(Json.obj(
        "id" -> note.id,
        "title" -> note.title,
        "subject" -> note.subject,
        "summary" -> note.summary,
        "created_at" -> note.createdAt.toString
      ))
    }
  }

  def updateNote(id: Long): Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { (userId, role) =>
      val titleOpt = (request.body \ "title").asOpt[String]
      val summaryOpt = (request.body \ "summary").asOpt[String]
      hiSchoolRepository.updateNote(id, titleOpt, summaryOpt) match {
        case Some(updated) => Ok(Json.obj("id" -> updated.id, "title" -> updated.title, "summary" -> updated.summary))
        case None => NotFound(Json.obj("detail" -> "Note not found."))
      }
    }
  }

  def deleteNote(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { (userId, role) =>
      if (hiSchoolRepository.deleteNote(id)) NoContent
      else NotFound(Json.obj("detail" -> "Note not found."))
    }
  }

  def verifyNote(id: Long): Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { (userId, role) =>
      // Strict RBAC: Only Teacher, SchoolAdmin, or PlatformAdmin can verify notes
      if (role != Role.Teacher.value && role != Role.SchoolAdmin.value && role != Role.PlatformAdmin.value) {
        Forbidden(Json.obj("detail" -> "Students cannot verify or modify teacher notes."))
      } else {
        val label = (request.body \ "verifiedByLabel").asOpt[String].orElse((request.body \ "verified_by").asOpt[String]).getOrElse("Faculty Chair")
        val comment = (request.body \ "comment").asOpt[String]

        hiSchoolRepository.verifyNote(id, verifierId = userId, verifierLabel = label, comment = comment) match {
          case Some(verified) =>
            Ok(Json.obj(
              "id" -> verified.id,
              "is_verified" -> verified.verified,
              "verified_by" -> verified.verifiedByLabel,
              "verified_at" -> verified.verifiedAt.map(_.toString),
              "verification_comment" -> verified.verificationComment
            ))
          case None => NotFound(Json.obj("detail" -> "Note not found."))
        }
      }
    }
  }

  def listQuizzes: Action[AnyContent] = Action {
    val quizzes = hiSchoolRepository.allQuizzes()
    val dtos = quizzes.map { q =>
      Json.obj(
        "id" -> q.id,
        "title" -> q.title,
        "subject" -> q.subject,
        "kind" -> q.kind,
        "questions_count" -> q.questionsCount,
        "estimated_minutes" -> q.estimatedMinutes,
        "mastery_score" -> q.masteryScore
      )
    }
    Ok(Json.toJson(dtos))
  }

  def getQuiz(id: Long): Action[AnyContent] = Action {
    hiSchoolRepository.findQuizById(id) match {
      case Some(q) =>
        val questionsDto = q.questions.map { item =>
          Json.obj(
            "id" -> item.id,
            "question" -> item.question,
            "options" -> item.options,
            "explanation" -> item.explanation
          )
        }
        Ok(Json.obj(
          "id" -> q.id,
          "title" -> q.title,
          "subject" -> q.subject,
          "kind" -> q.kind,
          "questions_count" -> q.questionsCount,
          "estimated_minutes" -> q.estimatedMinutes,
          "mastery_score" -> q.masteryScore,
          "questions" -> questionsDto
        ))
      case None => NotFound(Json.obj("detail" -> "Quiz not found."))
    }
  }

  def submitQuizAttempt(id: Long): Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { (userId, role) =>
      val answersOpt = (request.body \ "answers").asOpt[Map[String, Int]]
      answersOpt match {
        case Some(answers) =>
          hiSchoolRepository.evaluateAndSaveQuizAttempt(id, userId, answers) match {
            case Some(attempt) =>
              Ok(Json.obj(
                "attempt_id" -> attempt.id,
                "quiz_id" -> attempt.quizId,
                "score_percentage" -> attempt.scorePercentage,
                "total_questions" -> attempt.totalQuestions,
                "correct_count" -> attempt.correctCount,
                "attempted_at" -> attempt.createdAt.toString
              ))
            case None => NotFound(Json.obj("detail" -> "Quiz not found."))
          }
        case None => BadRequest(Json.obj("detail" -> "Answers payload required."))
      }
    }
  }

  def getQuizResults(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { (userId, role) =>
      val attempts = hiSchoolRepository.findQuizResultsByUser(userId).filter(_.quizId == id)
      val dtos = attempts.map { a =>
        Json.obj(
          "attempt_id" -> a.id,
          "quiz_id" -> a.quizId,
          "score_percentage" -> a.scorePercentage,
          "correct_count" -> a.correctCount,
          "total_questions" -> a.totalQuestions,
          "attempted_at" -> a.createdAt.toString
        )
      }
      Ok(Json.toJson(dtos))
    }
  }

  def listCircles: Action[AnyContent] = Action {
    val circles = hiSchoolRepository.allCircles()
    val dtos = circles.map { c =>
      Json.obj(
        "id" -> c.id,
        "name" -> c.name,
        "subject" -> c.subject,
        "next_session" -> c.nextSession,
        "is_live" -> c.isLive,
        "members_count" -> c.membersCount,
        "lead" -> "Dr. Aris Vance"
      )
    }
    Ok(Json.toJson(dtos))
  }

  def createCircle: Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { (userId, role) =>
      val name = (request.body \ "name").asOpt[String].getOrElse("New Study Circle")
      val subject = (request.body \ "subject").asOpt[String].getOrElse("General")
      val circle = hiSchoolRepository.createCircle(name, subject, leadId = userId)
      Ok(Json.obj(
        "id" -> circle.id,
        "name" -> circle.name,
        "subject" -> circle.subject,
        "next_session" -> circle.nextSession,
        "is_live" -> circle.isLive,
        "members_count" -> circle.membersCount
      ))
    }
  }

  def joinCircle(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { (userId, role) =>
      hiSchoolRepository.joinCircle(id, userId) match {
        case Some(c) => Ok(Json.obj("id" -> c.id, "members_count" -> c.membersCount, "joined" -> true))
        case None => NotFound(Json.obj("detail" -> "Circle not found."))
      }
    }
  }

  def leaveCircle(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { (userId, role) =>
      hiSchoolRepository.leaveCircle(id, userId) match {
        case Some(c) => Ok(Json.obj("id" -> c.id, "members_count" -> c.membersCount, "joined" -> false))
        case None => NotFound(Json.obj("detail" -> "Circle not found."))
      }
    }
  }

  def listCircleMembers(id: Long): Action[AnyContent] = Action {
    hiSchoolRepository.findCircleById(id) match {
      case Some(c) =>
        val members = c.memberUserIds.flatMap(userRepository.findById).map { u =>
          Json.obj("id" -> u.id, "name" -> s"${u.firstName} ${u.lastName}".trim, "email" -> u.email, "role" -> u.role.value)
        }
        Ok(Json.toJson(members))
      case None => NotFound(Json.obj("detail" -> "Circle not found."))
    }
  }

  def listDiscussions(circleId: Long): Action[AnyContent] = Action {
    val posts = hiSchoolRepository.findDiscussionsByCircle(circleId)
    val dtos = posts.map { p =>
      Json.obj(
        "id" -> s"disc_${p.id}",
        "circleId" -> s"sg${p.circleId}",
        "author" -> p.author,
        "authorInitials" -> p.authorInitials,
        "timestamp" -> p.timestamp,
        "content" -> p.content,
        "likes" -> p.likes
      )
    }
    Ok(Json.toJson(dtos))
  }

  def createDiscussion(circleId: Long): Action[JsValue] = Action(parse.json) { request =>
    val author = (request.body \ "author").asOpt[String].getOrElse("Student")
    val initials = (request.body \ "authorInitials").asOpt[String].getOrElse(author.take(2).toUpperCase)
    val content = (request.body \ "content").asOpt[String].getOrElse("")

    val post = hiSchoolRepository.createDiscussionPost(circleId, author, initials, content)
    Ok(Json.obj(
      "id" -> s"disc_${post.id}",
      "circleId" -> s"sg${post.circleId}",
      "author" -> post.author,
      "authorInitials" -> post.authorInitials,
      "timestamp" -> post.timestamp,
      "content" -> post.content,
      "likes" -> post.likes
    ))
  }

  def listPathways: Action[AnyContent] = Action {
    val pathways = hiSchoolRepository.allPathways()
    val dtos = pathways.map { p =>
      Json.obj(
        "id" -> p.id,
        "title" -> p.title,
        "alum_name" -> p.alumName,
        "grad_year" -> p.gradYear,
        "institution" -> p.institution,
        "quote" -> p.quote,
        "advice" -> p.advice,
        "electives" -> p.electives
      )
    }
    Ok(Json.toJson(dtos))
  }
}
