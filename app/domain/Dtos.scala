package domain

import play.api.libs.json._

case class LoginRequest(
    email: String,
    password: String
)

object LoginRequest:
  implicit val format: OFormat[LoginRequest] = Json.format[LoginRequest]

case class LoginResponse(
    access: String,
    refresh: String,
    user: UserSessionDto
)

object LoginResponse:
  implicit val format: OFormat[LoginResponse] = Json.format[LoginResponse]

case class UserSessionDto(
    id: String,
    email: String,
    name: String,
    role: String,
    school: String,
    grade: Option[String] = None,
    stream: Option[String] = None,
    studentId: Option[String] = None,
    avatarInitials: String = ""
)

object UserSessionDto:
  implicit val format: OFormat[UserSessionDto] = Json.format[UserSessionDto]

case class HealthResponse(
    status: String = "ok",
    service: String = "hicenter-scala-play"
)

object HealthResponse:
  implicit val format: OFormat[HealthResponse] = Json.format[HealthResponse]

case class ProvisionSchoolRequest(
    name: String,
    code: String,
    region: String,
    adminEmail: String,
    adminName: String
)

object ProvisionSchoolRequest:
  implicit val reads: Reads[ProvisionSchoolRequest] = new Reads[ProvisionSchoolRequest]:
    def reads(json: JsValue): JsResult[ProvisionSchoolRequest] =
      for
        name <- (json \ "name").validate[String]
        code <- (json \ "code").validate[String]
        region <- (json \ "region").validateOpt[String].map(_.getOrElse("Central Region"))
        adminEmail <- (json \ "adminEmail").validateOpt[String].orElse((json \ "admin_email").validateOpt[String]).map(_.getOrElse(""))
        adminName <- (json \ "adminName").validateOpt[String].orElse((json \ "admin_name").validateOpt[String]).map(_.getOrElse("School Admin"))
      yield ProvisionSchoolRequest(name, code, region, adminEmail, adminName)

  implicit val writes: OWrites[ProvisionSchoolRequest] = Json.writes[ProvisionSchoolRequest]

case class CreateTaskRequest(
    title: String,
    subject: Option[String],
    duePeriod: Option[String],
    due_period: Option[String],
    timeEstimate: Option[String],
    estimated_minutes: Option[Int],
    completed: Option[Boolean]
)

object CreateTaskRequest:
  implicit val reads: Reads[CreateTaskRequest] = new Reads[CreateTaskRequest]:
    def reads(json: JsValue): JsResult[CreateTaskRequest] =
      for
        title <- (json \ "title").validate[String]
        subject <- (json \ "subject").validateOpt[String]
        duePeriod <- (json \ "duePeriod").validateOpt[String]
        due_period <- (json \ "due_period").validateOpt[String]
        timeEstimate <- (json \ "timeEstimate").validateOpt[String]
        estimated_minutes <- (json \ "estimated_minutes").validateOpt[Int]
        completed <- (json \ "completed").validateOpt[Boolean]
      yield CreateTaskRequest(title, subject, duePeriod, due_period, timeEstimate, estimated_minutes, completed)

  implicit val writes: OWrites[CreateTaskRequest] = Json.writes[CreateTaskRequest]

case class UpdateTaskRequest(
    completed: Option[Boolean],
    title: Option[String],
    duePeriod: Option[String]
)

object UpdateTaskRequest:
  implicit val format: OFormat[UpdateTaskRequest] = Json.format[UpdateTaskRequest]

case class ActivateTokenRequest(
    token: String,
    password: String
)

object ActivateTokenRequest:
  implicit val format: OFormat[ActivateTokenRequest] = Json.format[ActivateTokenRequest]
