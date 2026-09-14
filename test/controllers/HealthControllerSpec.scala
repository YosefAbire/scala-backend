package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

class HealthControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting:

  "HealthController GET /api/health/" should {
    "return 200 OK with json status ok" in {
      val request = FakeRequest(GET, "/api/health/")
      val health = route(app, request).get

      status(health) mustBe OK
      contentType(health) mustBe Some("application/json")
      contentAsString(health) must include("hicenter-scala-play")
    }
  }
