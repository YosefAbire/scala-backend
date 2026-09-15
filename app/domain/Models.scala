package domain

import java.time.Instant
import play.api.libs.json._

enum Role(val value: String):
  case PlatformAdmin extends Role("platform_admin")
  case SchoolAdmin extends Role("school_admin")
  case Teacher extends Role("teacher")
  case Student extends Role("student")
  case Graduate extends Role("graduate")

object Role:
  def fromString(s: String): Role = s.toLowerCase match
    case "platform_admin" => PlatformAdmin
    case "school_admin"   => SchoolAdmin
    case "teacher"        => Teacher
    case "graduate"       => Graduate
    case _                => Student

  implicit val roleFormat: Format[Role] = new Format[Role]:
    def reads(json: JsValue): JsResult[Role] = json match
      case JsString(s) => JsSuccess(Role.fromString(s))
      case _           => JsError("Expected string for Role")
    def writes(role: Role): JsValue = JsString(role.value)

enum UserStatus(val value: String):
  case Active extends UserStatus("ACTIVE")
  case Invited extends UserStatus("INVITED")
  case Suspended extends UserStatus("SUSPENDED")
  case Graduated extends UserStatus("GRADUATED")
  case Deactivated extends UserStatus("DEACTIVATED")

object UserStatus:
  def fromString(s: String): UserStatus = s.toUpperCase match
    case "INVITED"     => Invited
    case "SUSPENDED"   => Suspended
    case "GRADUATED"   => Graduated
    case "DEACTIVATED" => Deactivated
    case _             => Active

  implicit val statusFormat: Format[UserStatus] = new Format[UserStatus]:
    def reads(json: JsValue): JsResult[UserStatus] = json match
      case JsString(s) => JsSuccess(UserStatus.fromString(s))
      case _           => JsError("Expected string for UserStatus")
    def writes(status: UserStatus): JsValue = JsString(status.value)

case class School(
    id: Long,
    name: String,
    code: String,
    isActive: Boolean = true,
    academicYearStart: Option[String] = None,
    academicYearEnd: Option[String] = None,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now()
)

object School:
  implicit val format: OFormat[School] = Json.format[School]

case class User(
    id: Long,
    username: String,
    email: String,
    passwordHash: String,
    role: Role,
    schoolId: Option[Long],
    status: UserStatus = UserStatus.Active,
    isActive: Boolean = true,
    firstName: String = "",
    lastName: String = "",
    createdAt: Instant = Instant.now()
)

object User:
  implicit val format: OFormat[User] = Json.format[User]

case class StudentProfile(
    id: Long,
    userId: Long,
    grade: Int,
    stream: String,
    enrollmentYear: Int
)

object StudentProfile:
  implicit val format: OFormat[StudentProfile] = Json.format[StudentProfile]

case class TeacherProfile(
    id: Long,
    userId: Long,
    subjects: String
)

object TeacherProfile:
  implicit val format: OFormat[TeacherProfile] = Json.format[TeacherProfile]

case class GraduateProfile(
    id: Long,
    userId: Long,
    university: String,
    major: String,
    whyChoseField: String = "",
    challenges: String = "",
    adviceForStudents: String = "",
    verifiedAt: Option[Instant] = None,
    verifiedById: Option[Long] = None,
    verificationSource: String = ""
)

object GraduateProfile:
  implicit val format: OFormat[GraduateProfile] = Json.format[GraduateProfile]

case class StudyNote(
    id: Long,
    schoolId: Long,
    authorId: Long,
    title: String,
    subject: String,
    chapter: String,
    summary: String,
    verified: Boolean = false,
    verifiedById: Option[Long] = None,
    verifiedByLabel: String = "",
    verifiedAt: Option[Instant] = None,
    verificationComment: Option[String] = None,
    downloadCount: Int = 0,
    createdAt: Instant = Instant.now()
)

object StudyNote:
  implicit val format: OFormat[StudyNote] = Json.format[StudyNote]

case class QuizQuestionItem(
    id: Int,
    question: String,
    options: List[String],
    correctOptionIndex: Int,
    explanation: String
)

object QuizQuestionItem:
  implicit val format: OFormat[QuizQuestionItem] = Json.format[QuizQuestionItem]

case class PracticeQuiz(
    id: Long,
    schoolId: Long,
    title: String,
    subject: String,
    kind: String = "formative",
    questionsCount: Int = 10,
    estimatedMinutes: Int = 15,
    masteryScore: Option[Int] = None,
    questions: List[QuizQuestionItem] = Nil,
    createdAt: Instant = Instant.now()
)

object PracticeQuiz:
  implicit val format: OFormat[PracticeQuiz] = Json.format[PracticeQuiz]

case class QuizAttempt(
    id: Long,
    quizId: Long,
    userId: Long,
    answers: Map[String, Int],
    scorePercentage: Int,
    totalQuestions: Int,
    correctCount: Int,
    createdAt: Instant = Instant.now()
)

object QuizAttempt:
  implicit val format: OFormat[QuizAttempt] = Json.format[QuizAttempt]

case class StudyCircle(
    id: Long,
    schoolId: Long,
    leadId: Long,
    name: String,
    subject: String,
    nextSession: String,
    isLive: Boolean = false,
    membersCount: Int = 1,
    memberUserIds: List[Long] = List(1L),
    createdAt: Instant = Instant.now()
)

object StudyCircle:
  implicit val format: OFormat[StudyCircle] = Json.format[StudyCircle]

case class GraduatePathway(
    id: Long,
    title: String,
    alumName: String,
    gradYear: String,
    institution: String,
    quote: String,
    advice: String,
    electives: List[String] = Nil,
    createdAt: Instant = Instant.now()
)

object GraduatePathway:
  implicit val format: OFormat[GraduatePathway] = Json.format[GraduatePathway]

case class TaskItem(
    id: Long,
    userId: Long,
    title: String,
    subject: String = "General",
    duePeriod: String = "Now",
    dueTime: String = "",
    timeEstimate: String = "30m",
    completed: Boolean = false,
    completedAt: Option[Instant] = None,
    notes: String = "",
    createdAt: Instant = Instant.now()
)

object TaskItem:
  implicit val format: OFormat[TaskItem] = Json.format[TaskItem]

case class FocusSession(
    id: Long,
    userId: Long,
    targetTaskId: Option[Long],
    mode: String = "pomodoro",
    durationMinutes: Int = 25,
    completed: Boolean = false,
    createdAt: Instant = Instant.now()
)

object FocusSession:
  implicit val format: OFormat[FocusSession] = Json.format[FocusSession]

case class RoutineItem(
    id: Long,
    userId: Long,
    kind: String,
    label: String,
    done: Boolean = false
)

object RoutineItem:
  implicit val format: OFormat[RoutineItem] = Json.format[RoutineItem]

case class DiscussionPost(
    id: Long,
    circleId: Long,
    author: String,
    authorInitials: String,
    content: String,
    likes: Int = 0,
    timestamp: String = "Just now"
)

object DiscussionPost:
  implicit val format: OFormat[DiscussionPost] = Json.format[DiscussionPost]

case class NotificationItem(
    id: Long,
    userId: Long,
    title: String,
    message: String,
    category: String = "academic",
    isRead: Boolean = false,
    createdAt: Instant = Instant.now()
)

object NotificationItem:
  implicit val format: OFormat[NotificationItem] = Json.format[NotificationItem]
