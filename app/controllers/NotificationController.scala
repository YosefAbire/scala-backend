package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import auth.JwtService
import repositories.NotificationRepository

@Singleton
class NotificationController @Inject() (
    cc: ControllerComponents,
    notificationRepository: NotificationRepository,
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
      case None => Unauthorized(Json.obj("detail" -> "Authentication required."))
    }
  }

  def listNotifications: Action[AnyContent] = Action { request =>
    withUser(request) { userId =>
      val items = notificationRepository.findByUser(userId)
      val dtos = items.map { item =>
        Json.obj(
          "id" -> item.id,
          "title" -> item.title,
          "message" -> item.message,
          "category" -> item.category,
          "is_read" -> item.isRead,
          "created_at" -> item.createdAt.toString
        )
      }
      Ok(Json.toJson(dtos))
    }
  }

  def markAsRead(id: Long): Action[AnyContent] = Action { request =>
    withUser(request) { userId =>
      notificationRepository.markAsRead(id, userId) match {
        case Some(updated) => Ok(Json.obj("id" -> updated.id, "is_read" -> updated.isRead))
        case None => NotFound(Json.obj("detail" -> "Notification not found."))
      }
    }
  }
}
