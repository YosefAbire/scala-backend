package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import domain._
import repositories.{HiSchoolRepository, UserRepository, AuditRepository}
import services.AiService
import auth.{JwtService, UserClaim}
import scala.concurrent.ExecutionContext

@Singleton
class HiSchoolController @Inject() (
    cc: ControllerComponents,
    hiSchoolRepository: HiSchoolRepository,
    userRepository: UserRepository,
    auditRepository: AuditRepository,
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

  private def withUser(request: Request[?])(block: UserClaim => Result): Result = {
    extractToken(request).flatMap(jwtService.validateToken) match {
      case Some(claim) => block(claim)
      case None => Unauthorized(Json.obj("detail" -> "Authentication required."))
    }
  }

  private def getUserSchoolId(claim: UserClaim): Long = {
    claim.schoolId.orElse {
      userRepository.findById(claim.userId).flatMap(_.schoolId)
    }.getOrElse(1L)
  }

  def listNotes: Action[AnyContent] = Action { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      val notes = if (claim.role == Role.PlatformAdmin.value) hiSchoolRepository.allNotes() else hiSchoolRepository.notesForSchool(schoolId)
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
  }

  def getNote(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      hiSchoolRepository.findNoteById(id) match {
        case Some(n) if claim.role == Role.PlatformAdmin.value || n.schoolId == schoolId =>
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
        case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
        case None => NotFound(Json.obj("detail" -> "Note not found."))
      }
    }
  }

  def createNote: Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      val title = (request.body \ "title").asOpt[String].getOrElse("New Study Note")
      val subject = (request.body \ "subject").asOpt[String].getOrElse("General")
      val summary = (request.body \ "summary").asOpt[String].getOrElse("Study note content summary.")

      val note = hiSchoolRepository.createNote(title, subject, summary, authorId = claim.userId, schoolId = schoolId)

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
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      hiSchoolRepository.findNoteById(id) match {
        case Some(n) if claim.role == Role.PlatformAdmin.value || n.schoolId == schoolId =>
          val titleOpt = (request.body \ "title").asOpt[String]
          val summaryOpt = (request.body \ "summary").asOpt[String]
          hiSchoolRepository.updateNote(id, titleOpt, summaryOpt) match {
            case Some(updated) => Ok(Json.obj("id" -> updated.id, "title" -> updated.title, "summary" -> updated.summary))
            case None => NotFound(Json.obj("detail" -> "Note not found."))
          }
        case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
        case None => NotFound(Json.obj("detail" -> "Note not found."))
      }
    }
  }

  def deleteNote(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      hiSchoolRepository.findNoteById(id) match {
        case Some(n) if claim.role == Role.PlatformAdmin.value || n.schoolId == schoolId =>
          if (hiSchoolRepository.deleteNote(id)) {
            auditRepository.record(
              actorId = claim.userId,
              actorEmail = claim.email,
              actorRole = claim.role,
              action = "DELETE_NOTE",
              schoolId = Some(n.schoolId),
              targetEntity = "StudyNote",
              targetId = Some(id),
              result = "SUCCESS"
            )
            NoContent
          } else NotFound(Json.obj("detail" -> "Note not found."))
        case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
        case None => NotFound(Json.obj("detail" -> "Note not found."))
      }
    }
  }

  def verifyNote(id: Long): Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { claim =>
      // Strict RBAC: Only Teacher, SchoolAdmin, or PlatformAdmin can verify notes
      if (claim.role != Role.Teacher.value && claim.role != Role.SchoolAdmin.value && claim.role != Role.PlatformAdmin.value) {
        Forbidden(Json.obj("detail" -> "Students cannot verify or modify teacher notes."))
      } else {
        val schoolId = getUserSchoolId(claim)
        hiSchoolRepository.findNoteById(id) match {
          case Some(n) if claim.role == Role.PlatformAdmin.value || n.schoolId == schoolId =>
            val label = (request.body \ "verifiedByLabel").asOpt[String].orElse((request.body \ "verified_by").asOpt[String]).getOrElse("Faculty Chair")
            val comment = (request.body \ "comment").asOpt[String]

            hiSchoolRepository.verifyNote(id, verifierId = claim.userId, verifierLabel = label, comment = comment) match {
              case Some(verified) =>
                auditRepository.record(
                  actorId = claim.userId,
                  actorEmail = claim.email,
                  actorRole = claim.role,
                  action = "VERIFY_NOTE",
                  schoolId = Some(verified.schoolId),
                  targetEntity = "StudyNote",
                  targetId = Some(verified.id),
                  result = "SUCCESS",
                  details = s"Verified note ${verified.title} by $label"
                )
                Ok(Json.obj(
                  "id" -> verified.id,
                  "is_verified" -> verified.verified,
                  "verified_by" -> verified.verifiedByLabel,
                  "verified_at" -> verified.verifiedAt.map(_.toString),
                  "verification_comment" -> verified.verificationComment
                ))
              case None => NotFound(Json.obj("detail" -> "Note not found."))
            }
          case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
          case None => NotFound(Json.obj("detail" -> "Note not found."))
        }
      }
    }
  }

  def listQuizzes: Action[AnyContent] = Action { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      val quizzes = if (claim.role == Role.PlatformAdmin.value) hiSchoolRepository.allQuizzes() else hiSchoolRepository.quizzesForSchool(schoolId)
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
  }

  def getQuiz(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      hiSchoolRepository.findQuizById(id) match {
        case Some(q) if claim.role == Role.PlatformAdmin.value || q.schoolId == schoolId =>
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
        case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
        case None => NotFound(Json.obj("detail" -> "Quiz not found."))
      }
    }
  }

  def submitQuizAttempt(id: Long): Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      hiSchoolRepository.findQuizById(id) match {
        case Some(q) if claim.role == Role.PlatformAdmin.value || q.schoolId == schoolId =>
          val answersOpt = (request.body \ "answers").asOpt[Map[String, Int]]
          answersOpt match {
            case Some(answers) =>
              hiSchoolRepository.evaluateAndSaveQuizAttempt(id, claim.userId, answers) match {
                case Some(attempt) =>
                  auditRepository.record(
                    actorId = claim.userId,
                    actorEmail = claim.email,
                    actorRole = claim.role,
                    action = "SUBMIT_QUIZ",
                    schoolId = Some(q.schoolId),
                    targetEntity = "PracticeQuiz",
                    targetId = Some(id),
                    result = "SUCCESS",
                    details = s"Score ${attempt.scorePercentage}% on quiz ${q.title}"
                  )
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
        case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
        case None => NotFound(Json.obj("detail" -> "Quiz not found."))
      }
    }
  }

  def getQuizResults(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { claim =>
      val attempts = hiSchoolRepository.findQuizResultsByUser(claim.userId).filter(_.quizId == id)
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

  def listCircles: Action[AnyContent] = Action { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      val circles = if (claim.role == Role.PlatformAdmin.value) hiSchoolRepository.allCircles() else hiSchoolRepository.circlesForSchool(schoolId)
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
  }

  def createCircle: Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      val name = (request.body \ "name").asOpt[String].getOrElse("New Study Circle")
      val subject = (request.body \ "subject").asOpt[String].getOrElse("General")
      val circle = hiSchoolRepository.createCircle(name, subject, leadId = claim.userId, schoolId = schoolId)
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
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      hiSchoolRepository.findCircleById(id) match {
        case Some(c) if claim.role == Role.PlatformAdmin.value || c.schoolId == schoolId =>
          hiSchoolRepository.joinCircle(id, claim.userId) match {
            case Some(joined) => Ok(Json.obj("id" -> joined.id, "members_count" -> joined.membersCount, "joined" -> true))
            case None => NotFound(Json.obj("detail" -> "Circle not found."))
          }
        case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
        case None => NotFound(Json.obj("detail" -> "Circle not found."))
      }
    }
  }

  def leaveCircle(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      hiSchoolRepository.findCircleById(id) match {
        case Some(c) if claim.role == Role.PlatformAdmin.value || c.schoolId == schoolId =>
          hiSchoolRepository.leaveCircle(id, claim.userId) match {
            case Some(left) => Ok(Json.obj("id" -> left.id, "members_count" -> left.membersCount, "joined" -> false))
            case None => NotFound(Json.obj("detail" -> "Circle not found."))
          }
        case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
        case None => NotFound(Json.obj("detail" -> "Circle not found."))
      }
    }
  }

  def listCircleMembers(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      hiSchoolRepository.findCircleById(id) match {
        case Some(c) if claim.role == Role.PlatformAdmin.value || c.schoolId == schoolId =>
          val members = c.memberUserIds.flatMap(userRepository.findById).map { u =>
            Json.obj("id" -> u.id, "name" -> s"${u.firstName} ${u.lastName}".trim, "email" -> u.email, "role" -> u.role.value)
          }
          Ok(Json.toJson(members))
        case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
        case None => NotFound(Json.obj("detail" -> "Circle not found."))
      }
    }
  }

  def listDiscussions(circleId: Long): Action[AnyContent] = Action { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      hiSchoolRepository.findCircleById(circleId) match {
        case Some(c) if claim.role == Role.PlatformAdmin.value || c.schoolId == schoolId =>
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
        case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
        case None => NotFound(Json.obj("detail" -> "Circle not found."))
      }
    }
  }

  def createDiscussion(circleId: Long): Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { claim =>
      val schoolId = getUserSchoolId(claim)
      hiSchoolRepository.findCircleById(circleId) match {
        case Some(c) if claim.role == Role.PlatformAdmin.value || c.schoolId == schoolId =>
          val author = (request.body \ "author").asOpt[String].getOrElse(s"${claim.email}")
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
        case Some(_) => Forbidden(Json.obj("detail" -> "Cross-tenant access forbidden."))
        case None => NotFound(Json.obj("detail" -> "Circle not found."))
      }
    }
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

