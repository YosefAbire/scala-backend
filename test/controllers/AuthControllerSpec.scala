package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json

class AuthControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting:

  "AuthController" should {

    "return 401 Unauthorized on /api/auth/me/ when unauthenticated" in {
      val request = FakeRequest(GET, "/api/auth/me/")
      val meResult = route(app, request).get

      status(meResult) mustBe UNAUTHORIZED
    }

    "return 200 OK on /api/auth/login/ with valid user credentials" in {
      val loginJson = Json.obj(
        "email" -> "scholar@academy.edu",
        "password" -> "password123"
      )
      val request = FakeRequest(POST, "/api/auth/login/").withJsonBody(loginJson)
      val loginResult = route(app, request).get

      status(loginResult) mustBe OK
      contentType(loginResult) mustBe Some("application/json")
      contentAsString(loginResult) must include("access")
      contentAsString(loginResult) must include("scholar@academy.edu")
    }

    "return 401 Unauthorized on /api/auth/login/ with invalid credentials" in {
      val loginJson = Json.obj(
        "email" -> "invalid@user.com",
        "password" -> "wrongpassword"
      )
      val request = FakeRequest(POST, "/api/auth/login/").withJsonBody(loginJson)
      val loginResult = route(app, request).get

      status(loginResult) mustBe UNAUTHORIZED
    }
  }
