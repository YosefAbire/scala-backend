package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import domain._
import repositories.HiTimeRepository

@Singleton
class HiTimeController @Inject() (
    cc: ControllerComponents,
    hiTimeRepository: HiTimeRepository
) extends AbstractController(cc) {

  def listTasks: Action[AnyContent] = Action {
    val tasks = hiTimeRepository.findTasksByUser(1)
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

  def createTask: Action[JsValue] = Action(parse.json) { request =>
    request.body.validate[CreateTaskRequest] match {
      case JsSuccess(req, _) =>
        val subject = req.subject.getOrElse("General")
        val duePeriod = req.duePeriod.orElse(req.due_period).getOrElse("Now")
        val est = req.timeEstimate.getOrElse(s"${req.estimated_minutes.getOrElse(25)} min")
        val task = hiTimeRepository.createTask(1, req.title, subject, duePeriod, est)
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

  def updateTask(id: Long): Action[JsValue] = Action(parse.json) { request =>
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

  def deleteTask(id: Long): Action[AnyContent] = Action {
    hiTimeRepository.deleteTask(id)
    NoContent
  }

  def listRoutines: Action[AnyContent] = Action {
    val routines = hiTimeRepository.findRoutinesByUser(1)
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

  def listSessions: Action[AnyContent] = Action {
    val sessions = hiTimeRepository.findSessionsByUser(1)
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
