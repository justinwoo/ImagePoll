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
    
    val pollResult = request.body.validate[Poll]
    pollResult.fold(
      errors => {
        Logger.debug("Warning! Poll not validating with the following error(s): " + errors)
        Future.successful(BadRequest(Json.obj("status" ->"validation-error", "message" -> JsError.toFlatJson(errors))))
      },
      poll => {
        println(poll)
        pollCollection.insert(poll).map { error =>
          if(error.inError)
            Logger.debug("Successfully inserted with error: " + error)
          else
            Logger.debug("Successfully inserted with no errors!")
          Created(Json.obj("id" -> poll.hashId))
        }
      }

    )
  }





}
