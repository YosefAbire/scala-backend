package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json

class QuizAssessmentSpec extends PlaySpec with GuiceOneAppPerTest with Injecting:

  private def loginAsStudent(): String = {
    val loginJson = Json.obj("email" -> "scholar@academy.edu", "password" -> "password123")
    val res = route(app, FakeRequest(POST, "/api/auth/login/").withJsonBody(loginJson)).get
    (Json.parse(contentAsString(res)) \ "access").as[String]
  }

  "HiSchoolController Quiz Assessment Lifecycle" should {

    "retrieve quiz details and submit attempt with backend score evaluation" in {
      val token = loginAsStudent()

      // 1. Get Quiz Details
      val getReq = FakeRequest(GET, "/api/hischool/quizzes/1/").withHeaders("Authorization" -> s"Bearer $token")
      val getRes = route(app, getReq).get
      status(getRes) mustBe OK
      contentAsString(getRes) must include("AP Chemistry Equilibrium")

      // 2. Submit Attempt (both answers correct)
      val attemptJson = Json.obj(
        "answers" -> Json.obj("1" -> 0, "2" -> 0)
      )
      val postReq = FakeRequest(POST, "/api/hischool/quizzes/1/attempts")
        .withHeaders("Authorization" -> s"Bearer $token", "Csrf-Token" -> "nocheck")
        .withJsonBody(attemptJson)
      val postRes = route(app, postReq).get
      status(postRes) mustBe OK
      val resJson = Json.parse(contentAsString(postRes))
      (resJson \ "score_percentage").as[Int] mustBe 100
      (resJson \ "correct_count").as[Int] mustBe 2

      // 3. Get Quiz Results
      val resultsReq = FakeRequest(GET, "/api/hischool/quizzes/1/results").withHeaders("Authorization" -> s"Bearer $token")
      val resultsRes = route(app, resultsReq).get
      status(resultsRes) mustBe OK
      contentAsString(resultsRes) must include("score_percentage")
    }
  }
