package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

class LearningLoopSpec extends PlaySpec with GuiceOneAppPerTest with Injecting:

  private def loginStudent(): String = {
    val loginJson = Json.obj("email" -> "scholar@academy.edu", "password" -> "password123")
    val res = route(app, FakeRequest(POST, "/api/auth/login/").withJsonBody(loginJson)).get
    (Json.parse(contentAsString(res)) \ "access").as[String]
  }

  "LearningLoopController" should {

    "fetch subject mastery for authenticated student" in {
      val token = loginStudent()
      val req = FakeRequest(GET, "/api/learning-loop/mastery").withHeaders("Authorization" -> s"Bearer $token")
      val res = route(app, req).get

      status(res) mustBe OK
      val json = Json.parse(contentAsString(res))
      json.as[Seq[JsValue]].nonEmpty mustBe true
      contentAsString(res) must include("AP Chemistry")
    }

    "fetch learning loop recommendations" in {
      val token = loginStudent()
      val req = FakeRequest(GET, "/api/learning-loop/recommendations").withHeaders("Authorization" -> s"Bearer $token")
      val res = route(app, req).get

      status(res) mustBe OK
      contentAsString(res) must include("recommendations")
    }

    "convert AI recommendation into HiTime task" in {
      val token = loginStudent()
      val convertPayload = Json.obj(
        "subject" -> "AP Chemistry",
        "taskTitle" -> "Review Equilibrium & Solve 3 Le Chatelier Problems",
        "duePeriod" -> "now",
        "estimatedMinutes" -> 30,
        "notes" -> "Learning Loop Focus Task"
      )
      val req = FakeRequest(POST, "/api/learning-loop/tasks/convert")
        .withHeaders("Authorization" -> s"Bearer $token")
        .withJsonBody(convertPayload)

      val res = route(app, req).get

      status(res) mustBe OK
      val json = Json.parse(contentAsString(res))
      (json \ "title").as[String] mustBe "Review Equilibrium & Solve 3 Le Chatelier Problems"
      (json \ "subject").as[String] mustBe "AP Chemistry"
    }
  }
