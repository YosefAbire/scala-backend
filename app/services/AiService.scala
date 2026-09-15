package services

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json._
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class AiService @Inject() (
    config: Configuration
)(implicit ec: ExecutionContext) {

  private val baseUrl: String = config.getOptional[String]("hicenter.ai.url").getOrElse("http://localhost:8001")
  private val secretKey: String = config.getOptional[String]("hicenter.ai.secret").getOrElse("hicenter-s2s-secret-key-change-in-production-32bytes")

  private val httpClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

  private def postJson(endpoint: String, payload: JsValue): Future[Either[String, JsValue]] = Future {
    try {
      val targetUri = URI.create(s"$baseUrl/api/v1$endpoint")
      val jsonString = Json.stringify(payload)

      val httpRequest = HttpRequest.newBuilder()
        .uri(targetUri)
        .header("Content-Type", "application/json")
        .header("X-Internal-Service-Key", secretKey)
        .POST(HttpRequest.BodyPublishers.ofString(jsonString))
        .timeout(Duration.ofSeconds(15))
        .build()

      val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        val parsed = Json.parse(response.body())
        Right(parsed)
      } else {
        Left(s"AI Service returned status ${response.statusCode()}: ${response.body()}")
      }
    } catch {
      case ex: Exception =>
        Left(s"Failed to connect to hicenter-ai service: ${ex.getMessage}")
    }
  }

  def chatAssistant(userId: Long, grade: Int, subject: String, messages: JsValue): Future[Either[String, JsValue]] = {
    val payload = Json.obj(
      "user_id" -> userId,
      "grade" -> grade,
      "subject" -> subject,
      "messages" -> messages,
      "include_rag_context" -> true
    )
    postJson("/ai/assistant/chat", payload)
  }

  def breakdownTask(userId: Long, goalTitle: String, subject: String): Future[Either[String, JsValue]] = {
    val payload = Json.obj(
      "user_id" -> userId,
      "goal_title" -> goalTitle,
      "subject" -> subject
    )
    postJson("/ai/tasks/breakdown", payload)
  }

  def searchRag(query: String, subject: Option[String], topK: Int): Future[Either[String, JsValue]] = {
    val payload = Json.obj(
      "query" -> query,
      "subject" -> subject,
      "top_k" -> topK
    )
    postJson("/ai/rag/search", payload)
  }

  def summarizeNote(title: String, content: String): Future[Either[String, JsValue]] = {
    val payload = Json.obj(
      "title" -> title,
      "content" -> content,
      "max_bullet_points" -> 5
    )
    postJson("/ai/nlp/summarize", payload)
  }

  def generateQuiz(subject: String, topic: String, questionsCount: Int): Future[Either[String, JsValue]] = {
    val payload = Json.obj(
      "subject" -> subject,
      "topic" -> topic,
      "questions_count" -> questionsCount
    )
    postJson("/ai/nlp/quiz-gen", payload)
  }

  def indexNoteChunk(noteId: Long, schoolId: Long, title: String, subject: String, chapter: String, content: String): Future[Either[String, JsValue]] = {
    val payload = Json.obj(
      "note_id" -> noteId,
      "school_id" -> schoolId,
      "title" -> title,
      "subject" -> subject,
      "chapter" -> chapter,
      "content" -> content
    )
    postJson("/ai/rag/index", payload)
  }

  def generateLearningLoopRecommendation(userId: Long, grade: Int, masteries: JsValue): Future[Either[String, JsValue]] = {
    val payload = Json.obj(
      "user_id" -> userId,
      "grade" -> grade,
      "masteries" -> masteries
    )
    postJson("/ai/recommendations/learning-loop", payload)
  }
}


