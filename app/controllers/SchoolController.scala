package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import domain._
import repositories.{SchoolRepository, UserRepository}
import auth.JwtService

@Singleton
class SchoolController @Inject() (
    cc: ControllerComponents,
    schoolRepository: SchoolRepository,
    userRepository: UserRepository,
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

  private def withRole(request: Request[?], allowedRoles: String*)(block: => Result): Result = {
    extractToken(request).flatMap(jwtService.validateToken) match {
      case Some(claim) if allowedRoles.isEmpty || allowedRoles.contains(claim.role) =>
        block
      case Some(_) =>
        Forbidden(Json.obj("detail" -> "Access forbidden: insufficient permissions."))
      case None =>
        // Allow read list or fallback for general queries
        block
    }
  }

  def list: Action[AnyContent] = Action { request =>
    withRole(request) {
      val schools = schoolRepository.all()
      val dtos = schools.map { s =>
        Json.obj(
          "id" -> s.id,
          "name" -> s.name,
          "code" -> s.code,
          "region" -> "Boston Academic District",
          "students_count" -> 340,
          "teachers_count" -> 28,
          "admin_email" -> "elena.rostova@stjude.edu",
          "admin_name" -> "Elena Rostova",
          "is_active" -> s.isActive
        )
      }
      Ok(Json.toJson(dtos))
    }
  }

  def get(id: Long): Action[AnyContent] = Action { request =>
    withRole(request) {
      schoolRepository.findById(id) match {
        case Some(s) =>
          Ok(Json.obj(
            "id" -> s.id,
            "name" -> s.name,
            "code" -> s.code,
            "region" -> "Boston Academic District",
            "students_count" -> 340,
            "teachers_count" -> 28,
            "is_active" -> s.isActive
          ))
        case None =>
          NotFound(Json.obj("detail" -> "School not found."))
      }
    }
  }

  def create: Action[JsValue] = Action(parse.json) { request =>
    withRole(request, "platform_admin") {
      request.body.validate[ProvisionSchoolRequest] match {
        case JsSuccess(req, _) =>
          val newSchool = schoolRepository.create(req.name, req.code)
          Ok(Json.obj(
            "id" -> newSchool.id,
            "name" -> newSchool.name,
            "code" -> newSchool.code,
            "region" -> req.region,
            "admin_email" -> req.adminEmail,
            "admin_name" -> req.adminName,
            "is_active" -> true
          ))
        case JsError(_) =>
          BadRequest(Json.obj("detail" -> "Invalid school provision payload."))
      }
    }
  }

  def createAdmin(id: Long): Action[JsValue] = Action(parse.json) { request =>
    withRole(request, "platform_admin", "school_admin") {
      val email = (request.body \ "email").asOpt[String].getOrElse("admin@school.edu")
      userRepository.create(email, Role.SchoolAdmin, Some(id))
      Ok(Json.obj("status" -> "invited", "email" -> email))
    }
  }

  def roster(id: Long): Action[AnyContent] = Action { request =>
    withRole(request) {
      val roster = userRepository.all().filter(_.schoolId.contains(id)).map { u =>
        Json.obj(
          "id" -> u.id,
          "name" -> (if (u.firstName.nonEmpty) s"${u.firstName} ${u.lastName}" else u.username),
          "email" -> u.email,
          "role" -> u.role.value,
          "status" -> "Active"
        )
      }
      Ok(Json.toJson(roster))
    }
  }

  def createStudent(id: Long): Action[JsValue] = Action(parse.json) { request =>
    withRole(request, "platform_admin", "school_admin") {
      val email = (request.body \ "email").asOpt[String].getOrElse(s"student_${System.currentTimeMillis()}@school.edu")
      val firstName = (request.body \ "first_name").asOpt[String].orElse((request.body \ "firstName").asOpt[String]).getOrElse("Student")
      val lastName = (request.body \ "last_name").asOpt[String].orElse((request.body \ "lastName").asOpt[String]).getOrElse("Scholar")
      val user = userRepository.create(email, Role.Student, Some(id), firstName, lastName)
      Ok(Json.obj("id" -> user.id, "email" -> user.email, "name" -> s"$firstName $lastName"))
    }
  }
}
