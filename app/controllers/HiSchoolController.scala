package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import domain._
import repositories.HiSchoolRepository
import services.AiService
import scala.concurrent.ExecutionContext

@Singleton
class HiSchoolController @Inject() (
    cc: ControllerComponents,
    hiSchoolRepository: HiSchoolRepository,
    aiService: AiService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

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
        "download_count" -> n.downloadCount,
        "author" -> "Faculty Chair",
        "created_at" -> n.createdAt.toString
      )
    }
    Ok(Json.toJson(dtos))
  }

  def createNote: Action[JsValue] = Action(parse.json) { request =>
    val title = (request.body \ "title").asOpt[String].getOrElse("New Study Note")
    val subject = (request.body \ "subject").asOpt[String].getOrElse("General")
    val summary = (request.body \ "summary").asOpt[String].getOrElse("Study note content summary.")

    val note = hiSchoolRepository.createNote(title, subject, summary)

    // Trigger automatic RAG vector indexing pipeline to hicenter-ai asynchronously
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
