package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import auth.JwtService
import services.AiService
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AiController @Inject() (
    cc: ControllerComponents,
    jwtService: JwtService,
    aiService: AiService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def extractToken(request: Request[?]): Option[String] = {
    request.cookies.get("hicenter_session").map(_.value).orElse {
      request.headers.get("Authorization").flatMap { auth =>
        if (auth.startsWith("Bearer ")) Some(auth.substring(7)) else None
      }
    }
  }

  private def withUser(request: Request[?])(block: (Long, String) => Future[Result]): Future[Result] = {
    extractToken(request).flatMap(jwtService.validateToken) match {
      case Some(claim) => block(claim.userId, claim.role)
      case None => Future.successful(Unauthorized(Json.obj("detail" -> "Authentication required for AI functionality.")))
    }
  }

  def chat: Action[JsValue] = Action.async(parse.json) { request =>
    withUser(request) { (userId, role) =>
      val subject = (request.body \ "subject").asOpt[String].getOrElse("General")
      val grade = (request.body \ "grade").asOpt[Int].getOrElse(12)
      val messages = (request.body \ "messages").asOpt[JsValue].getOrElse(Json.arr())

      aiService.chatAssistant(userId, grade, subject, messages).map {
        case Right(res) => Ok(res)
        case Left(err)  => BadGateway(Json.obj("detail" -> err))
      }
    }
  }

  def taskBreakdown: Action[JsValue] = Action.async(parse.json) { request =>
    withUser(request) { (userId, role) =>
      val goalTitle = (request.body \ "goal_title").asOpt[String].getOrElse("Study Session Goal")
      val subject = (request.body \ "subject").asOpt[String].getOrElse("General")

      aiService.breakdownTask(userId, goalTitle, subject).map {
        case Right(res) => Ok(res)
        case Left(err)  => BadGateway(Json.obj("detail" -> err))
      }
    }
  }

  def ragSearch: Action[JsValue] = Action.async(parse.json) { request =>
    withUser(request) { (userId, role) =>
      val query = (request.body \ "query").asOpt[String].getOrElse("")
      val subject = (request.body \ "subject").asOpt[String]
      val topK = (request.body \ "top_k").asOpt[Int].getOrElse(3)

      aiService.searchRag(query, subject, topK).map {
        case Right(res) => Ok(res)
        case Left(err)  => BadGateway(Json.obj("detail" -> err))
      }
    }
  }

  def summarizeNote: Action[JsValue] = Action.async(parse.json) { request =>
    withUser(request) { (userId, role) =>
      val title = (request.body \ "title").asOpt[String].getOrElse("Note Summary")
      val content = (request.body \ "content").asOpt[String].getOrElse("")

      aiService.summarizeNote(title, content).map {
        case Right(res) => Ok(res)
        case Left(err)  => BadGateway(Json.obj("detail" -> err))
      }
    }
  }

  def generateQuiz: Action[JsValue] = Action.async(parse.json) { request =>
    withUser(request) { (userId, role) =>
      val subject = (request.body \ "subject").asOpt[String].getOrElse("General")
      val topic = (request.body \ "topic").asOpt[String].getOrElse("General Topic")
      val count = (request.body \ "questions_count").asOpt[Int].getOrElse(5)

      aiService.generateQuiz(subject, topic, count).map {
        case Right(res) => Ok(res)
        case Left(err)  => BadGateway(Json.obj("detail" -> err))
      }
    }
  }
}
