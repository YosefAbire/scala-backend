package repositories

import javax.inject.{Inject, Singleton}
import domain._
import auth.JwtService
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

@Singleton
class UserRepository @Inject() (jwtService: JwtService):
  private val users = new ConcurrentHashMap[String, User]()
  private val idCounter = new java.util.concurrent.atomic.AtomicLong(100)

  // Seed default demo users
  private val defaultStudent = User(
    id = 1,
    username = "scholar@academy.edu",
    email = "scholar@academy.edu",
    passwordHash = jwtService.hashPassword("••••••••••••"),
    role = Role.Student,
    schoolId = Some(1),
    status = UserStatus.Active,
    firstName = "Maya",
    lastName = "Chen"
  )

  private val defaultSchoolAdmin = User(
    id = 2,
    username = "elena.rostova@stjude.edu",
    email = "elena.rostova@stjude.edu",
    passwordHash = jwtService.hashPassword("••••••••••••"),
    role = Role.SchoolAdmin,
    schoolId = Some(1),
    status = UserStatus.Active,
    firstName = "Elena",
    lastName = "Rostova"
  )

  private val defaultPlatformAdmin = User(
    id = 3,
    username = "platform@hicenter.local",
    email = "platform@hicenter.local",
    passwordHash = jwtService.hashPassword("••••••••••••"),
    role = Role.PlatformAdmin,
    schoolId = None,
    status = UserStatus.Active,
    firstName = "Platform",
    lastName = "Governance"
  )

  private val defaultTeacher = User(
    id = 4,
    username = "davies@stjude.edu",
    email = "davies@stjude.edu",
    passwordHash = jwtService.hashPassword("Password123!"),
    role = Role.Teacher,
    schoolId = Some(1),
    status = UserStatus.Active,
    firstName = "Arthur",
    lastName = "Davies"
  )

  users.put(defaultStudent.email, defaultStudent)
  users.put(defaultSchoolAdmin.email, defaultSchoolAdmin)
  users.put(defaultPlatformAdmin.email, defaultPlatformAdmin)
  users.put(defaultTeacher.email, defaultTeacher)

  def findByEmail(email: String): Option[User] =
    Option(users.get(email.toLowerCase.trim))

  def findById(id: Long): Option[User] =
    users.values().asScala.find(_.id == id)

  def save(user: User): User =
    users.put(user.email.toLowerCase.trim, user)
    user

  def create(email: String, role: Role, schoolId: Option[Long], firstName: String = "", lastName: String = ""): User =
    val id = idCounter.incrementAndGet()
    val newUser = User(
      id = id,
      username = email.toLowerCase.trim,
      email = email.toLowerCase.trim,
      passwordHash = jwtService.hashPassword("Password123!"),
      role = role,
      schoolId = schoolId,
      status = UserStatus.Invited,
      firstName = firstName,
      lastName = lastName
    )
    save(newUser)

  def updateMemberStatus(id: Long, newStatus: UserStatus): Option[User] =
    findById(id).map { u =>
      val updated = u.copy(status = newStatus, isActive = newStatus == UserStatus.Active || newStatus == UserStatus.Invited)
      save(updated)
    }

  def all(): Seq[User] =
    users.values().asScala.toSeq
