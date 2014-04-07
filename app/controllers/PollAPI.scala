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
import com.github.nscala_time.time.Imports._
import scala.util._

import models._
import models.JsonFormats._

object PollAPI extends Controller with MongoController {
  def pollCollection: JSONCollection = db.collection[JSONCollection]("polls")
  def voteCollection: JSONCollection = db.collection[JSONCollection]("votes")

  def getPollById (id: String) = Action.async {
    val futurePollOption: Future[Option[JsObject]] = pollCollection.find(Json.obj("hashId" -> id)).one[JsObject]  
    
    val pollFuture: Future[JsObject] = for { 
      option <- futurePollOption 
      p <- Future(option.getOrElse(Json.obj("error" ->"NotFound"))) 
    } yield p

    pollFuture.map { poll =>
      if (poll == Json.obj("error" -> "NotFound")) {
        Logger.info("Error: Poll Not found");
        NotFound(Json.obj("status" -> "not-found-error", "message" -> ("The requested poll was not found: " + poll.toString)))
      } else {
        Ok(poll)
      }
    } recover {
      case _ => BadRequest
    }
  }

  def createPollFromJson = Action.async(parse.json) { request =>
    Logger.info(request.toString)
    Logger.info(request.body.toString)
    
    val pollResult = request.body.validate[Poll]
    pollResult.fold(
      errors => {
        Logger.debug("Warning! Poll not validating with the following error(s): " + errors)
        Future.successful(BadRequest(Json.obj("status" ->"validation-error", "message" -> JsError.toFlatJson(errors))))
      },
      poll => {
        Logger.info(poll.toString)
        pollCollection.insert(poll).map { lastError =>
          if(lastError.inError)
            Logger.debug("Successfully inserted with error: " + lastError)
          else
            Logger.debug("Successfully inserted with no errors!")
          Created(Json.obj("id" -> poll.hashId))
        }
      }

    )
  }

  def createVoteFromJson (id: String) = Action.async(parse.json) { request =>
    var validParams = true
    var voteAnswers = List[Int]()

    val voteAnswersOption = (request.body \ "answerIdsToIncrement").asOpt[List[Int]]
    voteAnswersOption match {
      case Some(list) => voteAnswers = list
      case None => validParams = false
    }

    if (!validParams) {
      Logger.info("Error: Missing payload argument")
      Future.successful(BadRequest(Json.obj(
        "status" -> "validation-error",
        "message" -> Json.obj(
          "obj.answerIdsToIncrement" -> Json.obj(
            "msg" -> "error.path.missing",
            "args" -> Json.arr()
          )
        ))))
    } else {
      if (voteAnswers.distinct.length != voteAnswers.length) {
        Logger.info("Error: You cannot vote on the same answer more than once!")
        Future.successful(BadRequest(Json.obj(
          "status" -> "usage-error",
          "message" -> Json.obj(
            "obj.answerIdsToIncrement" -> Json.obj(
              "msg" -> "You cannot vote on the same answer more than once",
              "args" -> Json.arr()
            )
          ))))
      } else {
        val voteTime = DateTime.now
        val futurePollOption = pollCollection.find(Json.obj("hashId" -> id)).one[JsObject]

        val pollFuture: Future[JsObject] = for {
          option <- futurePollOption
          p <- Future(option.getOrElse(Json.obj("error" ->"NotFound")))
        } yield p

        pollFuture flatMap { poll =>
          if (poll == Json.obj("error" -> "NotFound")) {
            Logger.info("Error: Poll Not found");
            Future(NotFound(Json.obj("status" -> "not-found-error", "message" -> ("The requested poll was not found: " + poll.toString))))
          } else {
            val oldPoll = poll.as[Poll]

            if (voteTime.millis >= oldPoll.expirationDate.get.millis) {
              Future(BadRequest(Json.obj("status" -> "poll-expired")))
            } else {
              var answerIds = List[Int]()
              var validAnswerIds = true
              oldPoll.answers.foreach((answer: Answer) =>
                answerIds  = answerIds :+ answer.id
              )

              voteAnswers.foreach((i: Int) =>
                if (!answerIds.contains(i)) {
                  validAnswerIds = false
                }
              )

              if(!validAnswerIds) {
                Logger.info("Error: Invalid Answer Ids passed!")
                Future(BadRequest(Json.obj(
                  "status" -> "validation-error",
                  "message" -> Json.obj(
                    "obj.answerIdsToIncrement" -> Json.obj(
                      "msg" -> "error.invalid-id",
                      "args" -> Json.arr()
                    )
                  ))))
              } else {
                val prevVotesFuture = getVotesFutureByUserId(request.remoteAddress)
                prevVotesFuture map { prevVotes =>
                  if (prevVotes.length != 0) {
                    Logger.info("Erorr: Cannot vote more than once on a poll!")
                    BadRequest(Json.obj("status" -> "user-vote-error", "message" -> ("The current user has already voted on this poll. UserId:" + request.remoteAddress)))
                  } else {
                    val voteTest = Json.obj(
                      "pollId" -> id,
                      "userId" -> request.remoteAddress,
                      "answerIdsToIncrement" -> voteAnswers,
                      "voteTime" -> voteTime
                    )
                    voteCollection.insert(voteTest).map { lastError =>
                      if(lastError.inError)
                        Logger.info("Successfully inserted with error: " + lastError)
                      else
                        Logger.info("Successfully inserted with no errors!")                      
                    }
                    Created(voteTest)                    
                  }
                }
              }
            }
          }
        }
      }
    }  
  }

  def getPollResults (id: String) = Action.async {
    val pollFutureOption = getPollFutureByPollId(id)
    val pollFuture: Future[JsObject] = for {
      option <- pollFutureOption
      p <- Future(option.getOrElse(Json.obj("error" ->"NotFound")))
    } yield p

    pollFuture flatMap { poll =>
      if (poll == Json.obj("error" -> "NotFound")) {
        Logger.info("Error: Poll Not found");
        Future.successful(NotFound(Json.obj("status" -> "not-found-error", "message" -> ("The requested poll was not found: " + poll.toString))))
      } else {
        val voteFuture = getVotesFutureByPollId(id)
        voteFuture flatMap { votes =>
          println(votes)
          Future.successful(Ok(Json.obj("votes" -> votes)))
        }
      }
    }
  }

  def getVotesFutureByPollId (id: String): Future[List[JsObject]] = {
    val cursor: Cursor[JsObject] = voteCollection.find(Json.obj("pollId" -> id)).cursor[JsObject]
    cursor.collect[List]()
  }

  def getPollFutureByPollId (id: String): Future[Option[JsObject]] = {
    pollCollection.find(Json.obj("hashId" -> id)).one[JsObject]    
  }

  def getVotesFutureByUserId (id: String): Future[List[Vote]] = {
    val cursor: Cursor[Vote] = voteCollection.find(Json.obj("userId" -> id)).cursor[Vote]
    return cursor.collect[List]()
  }


}
