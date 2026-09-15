package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json

class RbacAuthorizationSpec extends PlaySpec with GuiceOneAppPerTest with Injecting:

  private def login(email: String, password: String): String = {
    val loginJson = Json.obj("email" -> email, "password" -> password)
    val res = route(app, FakeRequest(POST, "/api/auth/login/").withJsonBody(loginJson)).get
    (Json.parse(contentAsString(res)) \ "access").as[String]
  }

  "Platform Governance RBAC" should {

    "allow PlatformAdmin to access GET /api/platform/dashboard" in {
      val platformToken = login("platform@hicenter.local", "password123")
      val req = FakeRequest(GET, "/api/platform/dashboard").withHeaders("Authorization" -> s"Bearer $platformToken")
      val res = route(app, req).get

      status(res) mustBe OK
      contentAsString(res) must include("totalSchools")
      contentAsString(res) must include("platformStatus")
    }

    "reject Student access to GET /api/platform/dashboard with 403 Forbidden" in {
      val studentToken = login("scholar@academy.edu", "password123")
      val req = FakeRequest(GET, "/api/platform/dashboard").withHeaders("Authorization" -> s"Bearer $studentToken")
      val res = route(app, req).get

      status(res) mustBe FORBIDDEN
    }

    "allow SchoolAdmin to provision new staff members" in {
      val adminToken = login("elena.rostova@stjude.edu", "password123")
      val staffJson = Json.obj("email" -> "new.faculty@stjude.edu", "first_name" -> "Jane", "last_name" -> "Doe")
      val req = FakeRequest(POST, "/api/schools/1/staff/")
        .withHeaders("Authorization" -> s"Bearer $adminToken", "Csrf-Token" -> "nocheck")
        .withJsonBody(staffJson)
      val res = route(app, req).get

      status(res) mustBe OK
      contentAsString(res) must include("new.faculty@stjude.edu")
    }
  }
