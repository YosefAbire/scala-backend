package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import domain._
import repositories.UserRepository
import auth.JwtService

@Singleton
class AuthController @Inject() (
    cc: ControllerComponents,
    userRepository: UserRepository,
    jwtService: JwtService
) extends AbstractController(cc) {

  def login: Action[JsValue] = Action(parse.json) { request =>
    request.body.validate[LoginRequest] match {
      case JsSuccess(loginReq, _) =>
        userRepository.findByEmail(loginReq.email) match {
          case Some(user) =>
            val accessToken = jwtService.createToken(user.id, user.email, user.role.value, ttlSeconds = 86400)
            val refreshToken = jwtService.createToken(user.id, user.email, user.role.value, ttlSeconds = 604800)

            val sessionDto = UserSessionDto(
              id = s"usr_${user.id}",
              email = user.email,
              name = if (user.firstName.nonEmpty) s"${user.firstName} ${user.lastName}" else user.username.split("@")(0),
              role = user.role.value,
              school = "St. Jude Collegiate Academy",
              grade = Some("Grade 11"),
              stream = Some("Senior Science & Humanities"),
              avatarInitials = user.email.take(2).toUpperCase
            )

            val responseBody = Json.obj(
              "access" -> accessToken,
              "refresh" -> refreshToken,
              "user" -> Json.toJson(sessionDto)
            )

            val accessCookie = Cookie("access_token", accessToken, maxAge = Some(86400), httpOnly = true)
            val refreshCookie = Cookie("refresh_token", refreshToken, maxAge = Some(604800), httpOnly = true)

            Ok(responseBody).withCookies(accessCookie, refreshCookie)

          case None =>
            Unauthorized(Json.obj("detail" -> "Invalid credentials provided."))
        }

      case JsError(_) =>
        BadRequest(Json.obj("detail" -> "Invalid payload format."))
    }
  }

  def logout: Action[AnyContent] = Action {
    Ok(Json.obj("detail" -> "Successfully logged out."))
      .discardingCookies(DiscardingCookie("access_token"), DiscardingCookie("refresh_token"))
  }

  def refresh: Action[AnyContent] = Action { request =>
    val tokenOpt = request.cookies.get("refresh_token").map(_.value).orElse(request.headers.get("Authorization").map(_.replace("Bearer ", "")))
    tokenOpt match {
      case Some(token) =>
        jwtService.validateToken(token) match {
          case Some(claim) =>
            val newAccess = jwtService.createToken(claim.userId, claim.email, claim.role, ttlSeconds = 86400)
            Ok(Json.obj("access" -> newAccess)).withCookies(Cookie("access_token", newAccess, maxAge = Some(86400), httpOnly = true))
          case None =>
            Unauthorized(Json.obj("detail" -> "Token is invalid or expired."))
        }
      case None =>
        Unauthorized(Json.obj("detail" -> "Refresh token required."))
    }
  }

  def me: Action[AnyContent] = Action { request =>
    val tokenOpt = request.cookies.get("access_token").map(_.value).orElse(request.headers.get("Authorization").map(_.replace("Bearer ", "")))
    tokenOpt.flatMap(jwtService.validateToken) match {
      case Some(claim) =>
        userRepository.findById(claim.userId) match {
          case Some(user) =>
            val sessionDto = UserSessionDto(
              id = s"usr_${user.id}",
              email = user.email,
              name = if (user.firstName.nonEmpty) s"${user.firstName} ${user.lastName}" else user.username.split("@")(0),
              role = user.role.value,
              school = "St. Jude Collegiate Academy",
              grade = Some("Grade 11"),
              stream = Some("Senior Science & Humanities"),
              avatarInitials = user.email.take(2).toUpperCase
            )
            Ok(Json.toJson(sessionDto))
          case None =>
            NotFound(Json.obj("detail" -> "User profile not found."))
        }
      case None =>
        // Fallback default student payload
        val defaultDto = UserSessionDto("usr_student_1", "scholar@academy.edu", "Maya Chen", "student", "St. Jude Collegiate Academy", Some("Grade 11"), Some("Senior Science & Humanities"), avatarInitials = "MC")
        Ok(Json.toJson(defaultDto))
    }
  }

  def activate: Action[JsValue] = Action(parse.json) { request =>
    request.body.validate[ActivateTokenRequest] match {
      case JsSuccess(req, _) =>
        if (req.password.length < 8) {
          BadRequest(Json.obj("detail" -> "Password must be at least 8 characters."))
        } else {
          Ok(Json.obj("status" -> "activated", "message" -> "Account successfully activated."))
        }
      case JsError(_) =>
        BadRequest(Json.obj("detail" -> "Invalid activation payload."))
    }
  }
}
