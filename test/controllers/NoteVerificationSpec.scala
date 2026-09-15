package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json

class NoteVerificationSpec extends PlaySpec with GuiceOneAppPerTest with Injecting:

  private def login(email: String, password: String): String = {
    val loginJson = Json.obj("email" -> email, "password" -> password)
    val res = route(app, FakeRequest(POST, "/api/auth/login/").withJsonBody(loginJson)).get
    (Json.parse(contentAsString(res)) \ "access").as[String]
  }

  "HiSchoolController Note Verification RBAC" should {

    "allow Teacher role to verify a study note" in {
      val teacherToken = login("davies@stjude.edu", "Password123!")
      val verifyJson = Json.obj(
        "verifiedByLabel" -> "Mr. Davies (Faculty Chair)",
        "comment" -> "Verified for Grade 11 AP Chemistry curriculum."
      )
      val req = FakeRequest(POST, "/api/hischool/notes/1/verify")
        .withHeaders("Authorization" -> s"Bearer $teacherToken", "Csrf-Token" -> "nocheck")
        .withJsonBody(verifyJson)

      val res = route(app, req).get
      status(res) mustBe OK
      val resJson = Json.parse(contentAsString(res))
      (resJson \ "is_verified").as[Boolean] mustBe true
    }

    "reject Student role from verifying a study note with 403 Forbidden" in {
      val studentToken = login("scholar@academy.edu", "password123")
      val verifyJson = Json.obj("verifiedByLabel" -> "Unauthorized Verification Attempt")
      val req = FakeRequest(POST, "/api/hischool/notes/1/verify")
        .withHeaders("Authorization" -> s"Bearer $studentToken", "Csrf-Token" -> "nocheck")
        .withJsonBody(verifyJson)

      val res = route(app, req).get
      status(res) mustBe FORBIDDEN
    }
  }
