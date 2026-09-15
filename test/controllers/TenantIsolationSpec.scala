package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import auth.JwtService
import domain.Role

class TenantIsolationSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "Tenant Isolation Hardening" should {

    "reject School B user from accessing School A roster with 403 Forbidden" in {
      val jwtService = inject[JwtService]
      // Create user claim belonging to School 2
      val school2UserToken = jwtService.createToken(userId = 88, email = "admin@school2.edu", role = Role.SchoolAdmin.value, schoolId = Some(2L))

      val request = FakeRequest(GET, "/api/schools/1/roster/")
        .withHeaders("Authorization" -> s"Bearer $school2UserToken")

      val result = route(app, request).get
      status(result) mustBe FORBIDDEN
      (contentAsJson(result) \ "detail").as[String] must include("Cross-tenant access forbidden")
    }

    "reject School B user from accessing School A staff directory with 403 Forbidden" in {
      val jwtService = inject[JwtService]
      val school2TeacherToken = jwtService.createToken(userId = 89, email = "teacher@school2.edu", role = Role.Teacher.value, schoolId = Some(2L))

      val request = FakeRequest(GET, "/api/schools/1/staff/")
        .withHeaders("Authorization" -> s"Bearer $school2TeacherToken")

      val result = route(app, request).get
      status(result) mustBe FORBIDDEN
    }

    "reject School B user from fetching a note belonging exclusively to School A" in {
      val jwtService = inject[JwtService]
      // Note 1 belongs to School 1
      val school2StudentToken = jwtService.createToken(userId = 90, email = "student@school2.edu", role = Role.Student.value, schoolId = Some(2L))

      val request = FakeRequest(GET, "/api/hischool/notes/1/")
        .withHeaders("Authorization" -> s"Bearer $school2StudentToken")

      val result = route(app, request).get
      status(result) mustBe FORBIDDEN
    }

    "allow PlatformAdmin to access any school data across tenant boundaries" in {
      val jwtService = inject[JwtService]
      val platformAdminToken = jwtService.createToken(userId = 3, email = "platform@hicenter.local", role = Role.PlatformAdmin.value, schoolId = None)

      val request = FakeRequest(GET, "/api/schools/1/roster/")
        .withHeaders("Authorization" -> s"Bearer $platformAdminToken")

      val result = route(app, request).get
      status(result) mustBe OK
    }
  }
}
