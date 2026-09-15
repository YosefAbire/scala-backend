package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import auth.JwtService
import repositories.{HiSchoolRepository, HiTimeRepository}
import services.AiService
import domain._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LearningLoopController @Inject() (
    cc: ControllerComponents,
    jwtService: JwtService,
    hiSchoolRepository: HiSchoolRepository,
    hiTimeRepository: HiTimeRepository,
    aiService: AiService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def withUser(request: Request[?])(block: (Long, String) => Result): Result = {
    val authHeader = request.headers.get("Authorization")
    val token = authHeader.flatMap { h =>
      if (h.startsWith("Bearer ")) Some(h.substring(7)) else None
    }
    token.flatMap(jwtService.validateToken) match {
      case Some(claim) => block(claim.userId, claim.role)
      case None        => Unauthorized(Json.obj("detail" -> "Authentication required."))
    }
  }

  private def withUserAsync(request: Request[?])(block: (Long, String) => Future[Result]): Future[Result] = {
    val authHeader = request.headers.get("Authorization")
    val token = authHeader.flatMap { h =>
      if (h.startsWith("Bearer ")) Some(h.substring(7)) else None
    }
    token.flatMap(jwtService.validateToken) match {
      case Some(claim) => block(claim.userId, claim.role)
      case None        => Future.successful(Unauthorized(Json.obj("detail" -> "Authentication required.")))
    }
  }

  def getMastery: Action[AnyContent] = Action { request =>
    withUser(request) { (userId, _) =>
      val masteries = hiSchoolRepository.getUserMasteries(userId)
      Ok(Json.toJson(masteries))
    }
  }

  def getRecommendations: Action[AnyContent] = Action.async { request =>
    withUserAsync(request) { (userId, _) =>
      val masteries = hiSchoolRepository.getUserMasteries(userId)
      val masteriesJson = Json.toJson(masteries.map { m =>
        Json.obj(
          "subject" -> m.subject,
          "mastery_score" -> m.masteryScore,
          "weak_topics" -> m.weakTopics
        )
      })

      aiService.generateLearningLoopRecommendation(userId, grade = 12, masteriesJson).map {
        case Right(aiResponse) =>
          Ok(aiResponse)
        case Left(_) =>
          // Robust local fallback recommendation if hicenter-ai microservice is unreachable
          val fallbackRecs = masteries.map { m =>
            val topic = m.weakTopics.headOption.getOrElse(s"${m.subject} Fundamentals")
            val duePeriod = if (m.masteryScore < 75) "Now" else if (m.masteryScore < 85) "Next" else "Later"
            Json.obj(
              "subject" -> m.subject,
              "current_mastery" -> m.masteryScore,
              "focus_area" -> topic,
              "actionable_task_title" -> s"Review ${m.subject} notes & practice ${topic}",
              "recommended_duration_minutes" -> (if (m.masteryScore < 75) 45 else 30),
              "due_period" -> duePeriod,
              "rationale" -> s"Current mastery is ${m.masteryScore}%. Review ${topic} to elevate performance."
            )
          }
          Ok(Json.obj(
            "user_id" -> userId,
            "recommendations" -> fallbackRecs,
            "overall_loop_advice" -> "Fallback recommendation: Prioritize subjects with mastery under 80%."
          ))
      }
    }
  }

  def convertRecommendationToTask: Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { (userId, _) =>
      request.body.validate[ConvertRecommendationRequest] match {
        case JsSuccess(req, _) =>
          val task = hiTimeRepository.createTask(
            userId = userId,
            title = req.taskTitle,
            subject = req.subject,
            duePeriod = req.duePeriod.getOrElse("now"),
            timeEstimate = s"${req.estimatedMinutes.getOrElse(30)} min",
            notes = req.notes.getOrElse("AI Recommended Learning Loop Task")
          )
          Ok(Json.toJson(task))
        case JsError(errors) =>
          BadRequest(Json.obj("detail" -> "Invalid convert recommendation payload.", "errors" -> JsError.toJson(errors)))
      }
    }
  }
}
