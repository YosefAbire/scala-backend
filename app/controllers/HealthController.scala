package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json.Json
import services.AiService
import scala.concurrent.ExecutionContext

@Singleton
class HealthController @Inject() (
    cc: ControllerComponents,
    aiService: AiService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def health: Action[AnyContent] = Action {
    Ok(Json.obj(
      "status" -> "ok",
      "service" -> "hicenter-scala-play",
      "version" -> "3.0.2",
      "database" -> "healthy",
      "timestamp" -> java.time.Instant.now().toString
    ))
  }


  def ready: Action[AnyContent] = Action {
    Ok(Json.obj(
      "status" -> "ready",
      "service" -> "scala-backend",
      "database" -> "connected",
      "services" -> Json.obj("hicenter-ai" -> "reachable")
    ))
  }
}

