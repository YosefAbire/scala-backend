package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import domain._
import repositories.HiSchoolRepository

@Singleton
class HiSchoolController @Inject() (
    cc: ControllerComponents,
    hiSchoolRepository: HiSchoolRepository
) extends AbstractController(cc):

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
    Ok(Json.obj("id" -> System.currentTimeMillis(), "title" -> title, "subject" -> subject, "summary" -> summary))
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
