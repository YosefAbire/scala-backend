package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json.Json
import domain.HealthResponse

@Singleton
class HealthController @Inject() (cc: ControllerComponents) extends AbstractController(cc):
  def health: Action[AnyContent] = Action {
    Ok(Json.toJson(HealthResponse()))
  }
