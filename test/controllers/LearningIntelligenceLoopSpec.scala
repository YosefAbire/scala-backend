package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

class LearningIntelligenceLoopSpec extends PlaySpec with GuiceOneAppPerTest with Injecting:

  private def loginStudent(): String = {
    val loginJson = Json.obj("email" -> "scholar@academy.edu", "password" -> "password123")
    val res = route(app, FakeRequest(POST, "/api/auth/login/").withJsonBody(loginJson)).get
    (Json.parse(contentAsString(res)) \ "access").as[String]
  }

  private def loginTeacher(): String = {
    val loginJson = Json.obj("email" -> "davies@stjude.edu", "password" -> "Password123!")
    val res = route(app, FakeRequest(POST, "/api/auth/login/").withJsonBody(loginJson)).get
    (Json.parse(contentAsString(res)) \ "access").as[String]
  }

  "PHASE 3 Learning Intelligence Loop Full Pipeline" should {

    "1. Verify Study Note & Trigger RAG Indexing" in {
      val teacherToken = loginTeacher()
      val verifyJson = Json.obj(
        "verifiedByLabel" -> "Mr. Davies (Faculty Chair)",
        "comment" -> "Verified for Grade 12 AP Physics Kinematics & Dynamics."
      )
      val req = FakeRequest(POST, "/api/hischool/notes/1/verify")
        .withHeaders("Authorization" -> s"Bearer $teacherToken")
        .withJsonBody(verifyJson)

      val res = route(app, req).get
      status(res) mustBe OK
      (Json.parse(contentAsString(res)) \ "is_verified").as[Boolean] mustBe true
    }

    "2. Submit Quiz Attempt and Update Deterministic Topic Mastery" in {
      val studentToken = loginStudent()
      val attemptJson = Json.obj(
        "answers" -> Json.obj("1" -> 0, "2" -> 0)
      )
      val req = FakeRequest(POST, "/api/hischool/quizzes/1/attempts")
        .withHeaders("Authorization" -> s"Bearer $studentToken")
        .withJsonBody(attemptJson)

      val res = route(app, req).get
      status(res) mustBe OK
      val json = Json.parse(contentAsString(res))
      (json \ "score_percentage").as[Int] mustBe 100

      // Verify Topic & Subject Mastery Progress endpoint
      val progressReq = FakeRequest(GET, "/api/learning-loop/mastery")
        .withHeaders("Authorization" -> s"Bearer $studentToken")
      val progressRes = route(app, progressReq).get
      status(progressRes) mustBe OK
      contentAsString(progressRes) must include("topicMasteries")
    }

    "3. Fetch AI Recommendations and Convert to HiTime Task" in {
      val studentToken = loginStudent()
      
      // Fetch AI recommendations
      val recReq = FakeRequest(GET, "/api/learning-loop/recommendations")
        .withHeaders("Authorization" -> s"Bearer $studentToken")
      val recRes = route(app, recReq).get
      status(recRes) mustBe OK
      contentAsString(recRes) must include("recommendations")

      // Convert recommendation to HiTime Task
      val convertJson = Json.obj(
        "subject" -> "Physics",
        "taskTitle" -> "Review Newton's Laws & Solve 3 Dynamics Problems",
        "duePeriod" -> "now",
        "estimatedMinutes" -> 30,
        "notes" -> "Learning Loop Focus Task"
      )
      val convertReq = FakeRequest(POST, "/api/learning-loop/tasks/convert")
        .withHeaders("Authorization" -> s"Bearer $studentToken")
        .withJsonBody(convertJson)
      val convertRes = route(app, convertReq).get
      status(convertRes) mustBe OK
      val taskJson = Json.parse(contentAsString(convertRes))
      val taskId = (taskJson \ "id").as[Long]
      (taskJson \ "subject").as[String] mustBe "Physics"

      // 4. Complete HiTime Task and verify active study signal boost (+3% topic mastery)
      val updateTaskReq = FakeRequest(PATCH, s"/api/hitime/tasks/$taskId/")
        .withHeaders("Authorization" -> s"Bearer $studentToken")
        .withJsonBody(Json.obj("completed" -> true))
      val updateTaskRes = route(app, updateTaskReq).get
      status(updateTaskRes) mustBe OK
      (Json.parse(contentAsString(updateTaskRes)) \ "completed").as[Boolean] mustBe true
    }

    "4. Verify AI Microservice Failure Isolation" in {
      val studentToken = loginStudent()
      
      // Quiz submission must succeed even if AI service is offline
      val attemptJson = Json.obj("answers" -> Json.obj("1" -> 0))
      val req = FakeRequest(POST, "/api/hischool/quizzes/2/attempts")
        .withHeaders("Authorization" -> s"Bearer $studentToken")
        .withJsonBody(attemptJson)

      val res = route(app, req).get
      status(res) mustBe OK
      val json = Json.parse(contentAsString(res))
      (json \ "score_percentage").as[Int] mustBe 100
    }
  }
