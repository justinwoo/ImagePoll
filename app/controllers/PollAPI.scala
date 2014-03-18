package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scala.concurrent.Future
import reactivemongo.api._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

import models._
import models.JsonFormats._

object PollAPI extends Controller with MongoController {
  def pollCollection: JSONCollection = db.collection[JSONCollection]("polls")

  def getPollById (id: String) = Action.async {
    val futurePollOption: Future[Option[JsObject]] = pollCollection.find(Json.obj("hashId" -> id)).one[JsObject]  
    
    val pollFuture: Future[JsObject] = for { 
      option <- futurePollOption 
      p <- Future(option.getOrElse(Json.obj("error" ->"NotFound"))) 
    } yield p

    pollFuture.map { poll =>
      if (poll == Json.obj("error" ->"NotFound")) {
        println("Error: Poll Not found");
        NotFound(poll)
      } else {
        Ok(poll)
      }
    } recover {
      case _ => BadRequest
    }
  }

  def createPollFromJson = Action.async(parse.json) { request =>
    println(request)
    println(request.body)
    request.body.validate[Poll].map { poll =>
      println(poll)
      pollCollection.insert(poll).map { error =>
        Logger.debug("Successfully inserted with error: " + error)
        Created(Json.obj("id" -> poll.hashId))
      }   
    }.getOrElse(Future.successful(BadRequest("invalid json payload"))) //TODO: more detailed errors
  }

}
