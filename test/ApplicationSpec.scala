import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import play.api._
import play.modules.reactivemongo.json.collection.JSONCollection
import com.github.nscala_time.time.Imports._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import scala.concurrent._
import duration._
import play.api.libs.json._
import play.api.data._
import scala.util._

import models._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {
  val testHashId = "Abc12"

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) must beNone
    }

    //TODO: check mongo connection is up

    "send 400 on improper payload to /polls" in new WithApplication {
      val newPoll = Json.obj(
        "InvalidKey" -> "InvalidValue"
      )

      val result = controllers.PollAPI.createPollFromJson()(FakeRequest(POST, "/polls", FakeHeaders(), newPoll))

      status(result) must equalTo(400)
      contentType(result) must beSome("application/json")
    }

    "send a 201 response when calling POST /polls with a valid payload" in new WithApplication{
      val newPoll = Json.obj(
        "hashId" -> testHashId,
        "title" -> "Cats v. Dogs",
        "questionText" -> "Who is cooler? Cats or Dogs?",
        "expirationDate" -> Option(DateTime.now + 2.months),
        "createdDate" -> Option(DateTime.now),
        "answers" -> Json.arr(
            Json.obj(
              "id" -> 1,
              "text" -> "cats are cooler",
              "s3ImageId" -> 0
            ), Json.obj(
              "id" ->2,
              "text" ->"dogs are cooler",
              "s3ImageId" -> 0
            )),
        "votingSystem" -> Json.obj(
          "votingType" -> "selection",
          "n" -> 1
        )
      )
      val result = controllers.PollAPI.createPollFromJson()(FakeRequest(POST, "/polls", FakeHeaders(), newPoll))

      status(result) must equalTo(201)
      contentType(result) must beSome("application/json")
    }
  }

  "send 404 on GET /polls/:id if poll does not exist" in new WithApplication{
    val invalidHash = "-1"
    val result = controllers.PollAPI.getPollById(invalidHash)(FakeRequest())

    status(result) must equalTo(404)
    contentType(result) must beSome("application/json")
  }

  "send a 200 response plus the poll data when calling GET /polls/:id with a valid hashId" in new WithApplication{
    val result = controllers.PollAPI.getPollById(testHashId)(FakeRequest())

    status(result) must equalTo(200)
    contentType(result) must beSome("application/json")
  }

  "send a 400 response on improper payload to POST /polls/:id/votes" in new WithApplication{
    val newVote = Json.obj(
      "InvalidKey" -> "InvalidValue"
    )
    val result = controllers.PollAPI.createVoteFromJson(testHashId)(FakeRequest(POST, "/polls/" + testHashId + "/votes" , FakeHeaders(), newVote))
    status(result) must equalTo(400)
    contentType(result) must beSome("application/json")
  }

  "send a 400 response on proper payload but expired poll" in new WithApplication{
    val expiredPoll = Json.obj(
      "hashId" -> "expiredId",
      "title" -> "Cats v. Dogs",
      "questionText" -> "Who is cooler? Cats or Dogs?",
      "expirationDate" -> Option(DateTime.now - 2.months),
      "createdDate" -> Option(DateTime.now),
      "answers" -> Json.arr(
        Json.obj(
          "id" -> 1,
          "text" -> "cats are cooler",
          "s3ImageId" -> 0
        ), Json.obj(
              "id" ->2,
          "text" ->"dogs are cooler",
          "s3ImageId" -> 0
        )),
      "votingSystem" -> Json.obj(
        "votingType" -> "selection",
        "n" -> 1
      )
    )
    var result = controllers.PollAPI.createPollFromJson()(FakeRequest(POST, "/polls", FakeHeaders(), expiredPoll))
    status(result) must equalTo(201)

    val newVote = Json.obj(
      "answerIdsToIncrement" -> List(1, 2)
    )
    result = controllers.PollAPI.createVoteFromJson("expiredId")(FakeRequest(POST, "/polls/" + "expiredId" + "/votes" , FakeHeaders(), newVote))
    status(result) must equalTo(400)
    contentType(result) must beSome("application/json")
  }


  "send a 400 response on trying to vote on the same answer more than once" in new WithApplication{
    val newVote = Json.obj(
      "answerIdsToIncrement" -> List(1, 2, 1)
    )
    val result = controllers.PollAPI.createVoteFromJson(testHashId)(FakeRequest(POST, "/polls/" + testHashId + "/votes" , FakeHeaders(), newVote))
    status(result) must equalTo(400)
    contentType(result) must beSome("application/json")  
  }

  "send a 404 response on proper payload but unknown /poll/:id" in new WithApplication{
    val invalidHash = "-1"
    val newVote = Json.obj(
      "answerIdsToIncrement" -> List(1, 2)
    )
    val result = controllers.PollAPI.createVoteFromJson(invalidHash)(FakeRequest(POST, "/polls/" + invalidHash + "/votes" , FakeHeaders(), newVote))
    status(result) must equalTo(404)
    contentType(result) must beSome("application/json")  
  }

  "send a 201 response on valid payload to POST /polls/:id/votes" in new WithApplication{
    val newVote = Json.obj(
      "answerIdsToIncrement" -> List(1, 2)
    )
    val result = controllers.PollAPI.createVoteFromJson(testHashId)(FakeRequest(POST, "/polls/" + testHashId + "/votes" , FakeHeaders(), newVote))
    status(result) must equalTo(201)
    contentType(result) must beSome("application/json")  
  }

  //TODO: Send a 400 on valid payload but this ip address has already voted on this poll

}
