package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import auth.JwtService
import domain.Role
import repositories.AuditRepository

class ProductionHardeningEndToEndSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "PHASE 5 Production Hardening End-to-End Scenario" should {

    "execute complete multi-persona workflow with security, auditing, and tenant isolation" in {
      val jwtService = inject[JwtService]
      val auditRepository = inject[AuditRepository]

      // 1. PlatformAdmin provisions a new school
      val platformAdminToken = jwtService.createToken(3, "platform@hicenter.local", Role.PlatformAdmin.value, None)
      val provisionReq = FakeRequest(POST, "/api/schools/")
        .withHeaders("Authorization" -> s"Bearer $platformAdminToken")
        .withJsonBody(Json.obj("name" -> "Horizon Tech High", "code" -> "HORIZON", "region" -> "Cambridge East", "adminEmail" -> "admin@horizon.edu", "adminName" -> "Sarah Connor"))

      val provisionRes = route(app, provisionReq).get
      status(provisionRes) mustBe OK
      val schoolId = (contentAsJson(provisionRes) \ "id").as[Long]

      // 2. SchoolAdmin invites teacher and enrolls student for the new school
      val schoolAdminToken = jwtService.createToken(50, "admin@horizon.edu", Role.SchoolAdmin.value, Some(schoolId))
      
      val inviteTeacherReq = FakeRequest(POST, s"/api/schools/$schoolId/staff/")
        .withHeaders("Authorization" -> s"Bearer $schoolAdminToken")
        .withJsonBody(Json.obj("email" -> "prof.vance@horizon.edu", "first_name" -> "Aris", "last_name" -> "Vance"))
      val inviteTeacherRes = route(app, inviteTeacherReq).get
      status(inviteTeacherRes) mustBe OK
      val teacherId = (contentAsJson(inviteTeacherRes) \ "id").as[Long]

      val enrollStudentReq = FakeRequest(POST, s"/api/schools/$schoolId/students/")
        .withHeaders("Authorization" -> s"Bearer $schoolAdminToken")
        .withJsonBody(Json.obj("email" -> "alex.student@horizon.edu", "first_name" -> "Alex", "last_name" -> "Rivera"))
      val enrollStudentRes = route(app, enrollStudentReq).get
      status(enrollStudentRes) mustBe OK
      val studentId = (contentAsJson(enrollStudentRes) \ "id").as[Long]

      // 3. Teacher creates and verifies a study note
      val teacherToken = jwtService.createToken(teacherId, "prof.vance@horizon.edu", Role.Teacher.value, Some(schoolId))
      val createNoteReq = FakeRequest(POST, "/api/hischool/notes/")
        .withHeaders("Authorization" -> s"Bearer $teacherToken")
        .withJsonBody(Json.obj("title" -> "AP Physics Quantum Optics", "subject" -> "Physics", "summary" -> "Comprehensive wave function optics derivations."))
      val createNoteRes = route(app, createNoteReq).get
      status(createNoteRes) mustBe OK
      val noteId = (contentAsJson(createNoteRes) \ "id").as[Long]

      val verifyNoteReq = FakeRequest(POST, s"/api/hischool/notes/$noteId/verify")
        .withHeaders("Authorization" -> s"Bearer $teacherToken")
        .withJsonBody(Json.obj("verifiedByLabel" -> "Dr. Aris Vance (Department Chair)", "comment" -> "Verified accurate."))
      val verifyNoteRes = route(app, verifyNoteReq).get
      status(verifyNoteRes) mustBe OK
      (contentAsJson(verifyNoteRes) \ "is_verified").as[Boolean] mustBe true

      // 4. Student accesses note, takes quiz, converts recommendation to HiTime task
      val studentToken = jwtService.createToken(studentId, "alex.student@horizon.edu", Role.Student.value, Some(schoolId))
      val getNoteReq = FakeRequest(GET, s"/api/hischool/notes/$noteId/")
        .withHeaders("Authorization" -> s"Bearer $studentToken")
      val getNoteRes = route(app, getNoteReq).get
      status(getNoteRes) mustBe OK

      val submitQuizReq = FakeRequest(POST, "/api/hischool/quizzes/1/attempts")
        .withHeaders("Authorization" -> s"Bearer $studentToken")
        .withJsonBody(Json.obj("answers" -> Map("1" -> 0, "2" -> 0)))
      val submitQuizRes = route(app, submitQuizReq).get
      status(submitQuizRes) mustBe OK
      (contentAsJson(submitQuizRes) \ "score_percentage").as[Int] mustBe 100

      val convertTaskReq = FakeRequest(POST, "/api/learning-loop/tasks/convert")
        .withHeaders("Authorization" -> s"Bearer $studentToken")
        .withJsonBody(Json.obj("subject" -> "Physics", "taskTitle" -> "Quantum Optics Sprint", "estimatedMinutes" -> 45))
      val convertTaskRes = route(app, convertTaskReq).get
      status(convertTaskRes) mustBe OK

      // 5. Graduate accesses pathway functionality
      val gradToken = jwtService.createToken(99, "alum@horizon.edu", Role.Graduate.value, Some(schoolId))
      val listPathwaysReq = FakeRequest(GET, "/api/hischool/pathways/")
        .withHeaders("Authorization" -> s"Bearer $gradToken")
      val listPathwaysRes = route(app, listPathwaysReq).get
      status(listPathwaysRes) mustBe OK

      // 6. Verify Security Audit Trail
      val auditLogs = auditRepository.all()
      auditLogs.exists(_.action == "CREATE_SCHOOL") mustBe true
      auditLogs.exists(_.action == "INVITE_TEACHER") mustBe true
      auditLogs.exists(_.action == "ENROLL_STUDENT") mustBe true
      auditLogs.exists(_.action == "VERIFY_NOTE") mustBe true
      auditLogs.exists(_.action == "SUBMIT_QUIZ") mustBe true
    }
  }
}
