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
        NotFound("404; Poll Not Found")
      } else {
        Ok(poll)
      }
    } recover {
      case _ => BadRequest
    }
  }

}
