package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import domain._
import repositories.HiTimeRepository
import auth.JwtService

@Singleton
class HiTimeController @Inject() (
    cc: ControllerComponents,
    hiTimeRepository: HiTimeRepository,
    jwtService: JwtService
) extends AbstractController(cc) {

  private def extractToken(request: Request[?]): Option[String] = {
    request.cookies.get("access_token").map(_.value).orElse {
      request.cookies.get("hicenter_session").map(_.value).orElse {
        request.headers.get("Authorization").flatMap { auth =>
          if (auth.startsWith("Bearer ")) Some(auth.substring(7)) else None
        }
      }
    }
  }

  private def withUser(request: Request[?])(block: Long => Result): Result = {
    extractToken(request).flatMap(jwtService.validateToken) match {
      case Some(claim) => block(claim.userId)
      case None =>
        // Default to user ID 1 for legacy/unauthenticated session support if token absent
        block(1)
    }
  }

  def listTasks: Action[AnyContent] = Action { request =>
    withUser(request) { userId =>
      val tasks = hiTimeRepository.findTasksByUser(userId)
      val dtos = tasks.map { t =>
        Json.obj(
          "id" -> t.id,
          "title" -> t.title,
          "subject" -> t.subject,
          "due_period" -> t.duePeriod,
          "due_time" -> t.dueTime,
          "estimated_minutes" -> 25,
          "completed" -> t.completed,
          "notes" -> t.notes
        )
      }
      Ok(Json.toJson(dtos))
    }
  }

  def createTask: Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { userId =>
      request.body.validate[CreateTaskRequest] match {
        case JsSuccess(req, _) =>
          val subject = req.subject.getOrElse("General")
          val duePeriod = req.duePeriod.orElse(req.due_period).getOrElse("Now")
          val est = req.timeEstimate.getOrElse(s"${req.estimated_minutes.getOrElse(25)} min")
          val task = hiTimeRepository.createTask(userId, req.title, subject, duePeriod, est)
          Ok(Json.obj(
            "id" -> task.id,
            "title" -> task.title,
            "subject" -> task.subject,
            "due_period" -> task.duePeriod,
            "estimated_minutes" -> 25,
            "completed" -> task.completed
          ))
        case JsError(_) =>
          BadRequest(Json.obj("detail" -> "Invalid task payload."))
      }
    }
  }

  def updateTask(id: Long): Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { userId =>
      val completedOpt = (request.body \ "completed").asOpt[Boolean]
      completedOpt match {
        case Some(comp) =>
          hiTimeRepository.updateTaskStatus(id, comp) match {
            case Some(updated) =>
              Ok(Json.obj("id" -> updated.id, "completed" -> updated.completed))
            case None =>
              NotFound(Json.obj("detail" -> "Task not found."))
          }
        case None =>
          Ok(Json.obj("id" -> id))
      }
    }
  }

  def deleteTask(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { userId =>
      hiTimeRepository.deleteTask(id)
      NoContent
    }
  }

  def listRoutines: Action[AnyContent] = Action { request =>
    withUser(request) { userId =>
      val routines = hiTimeRepository.findRoutinesByUser(userId)
      val dtos = routines.map { r =>
        Json.obj(
          "id" -> r.id,
          "kind" -> r.kind,
          "label" -> r.label,
          "done" -> r.done
        )
      }
      Ok(Json.toJson(dtos))
    }
  }

  def updateRoutine(id: Long): Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { userId =>
      val doneOpt = (request.body \ "done").asOpt[Boolean]
      doneOpt match {
        case Some(d) =>
          hiTimeRepository.toggleRoutineStatus(id, d) match {
            case Some(updated) => Ok(Json.obj("id" -> updated.id, "done" -> updated.done))
            case None => Ok(Json.obj("id" -> id, "done" -> d))
          }
        case None =>
          Ok(Json.obj("id" -> id))
      }
    }
  }

  def listSessions: Action[AnyContent] = Action { request =>
    withUser(request) { userId =>
      val sessions = hiTimeRepository.findSessionsByUser(userId)
      val dtos = sessions.map { s =>
        Json.obj(
          "id" -> s.id,
          "mode" -> s.mode,
          "duration_minutes" -> s.durationMinutes,
          "completed" -> s.completed
        )
      }
      Ok(Json.toJson(dtos))
    }
  }

  def createSession: Action[JsValue] = Action(parse.json) { request =>
    withUser(request) { userId =>
      val mode = (request.body \ "mode").asOpt[String].getOrElse("pomodoro")
      val duration = (request.body \ "duration_minutes").asOpt[Int].getOrElse(25)
      val session = hiTimeRepository.createFocusSession(userId, mode, duration)
      Ok(Json.obj("id" -> session.id, "mode" -> session.mode, "duration_minutes" -> session.durationMinutes, "completed" -> session.completed))
    }
  }
}

