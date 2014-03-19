import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import play.api._
import play.modules.reactivemongo.json.collection.JSONCollection
import com.github.nscala_time.time.Imports._

import reactivemongo.api._
import scala.concurrent._
import duration._
import play.api.libs.json._
import play.api.data._

import models._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) must beNone
    }

    //TODO: check mongo connection is up

    //TODO: test failing on sending improper payload to /polls

    //TODO: test passing on sending proper formatted payload to /polls

    "send a 201 response when calling POST /polls with a valid payload" in new WithApplication{
      val newPoll = Json.obj(
        "hashId" -> "5322327974b1a575b3c88b1835",
        "title" -> "Cats v. Dogs",
        "questionText" -> "Who is cooler? Cats or Dogs?",
        "expirationDate" -> Option(DateTime.now + 2.months),
        "createdDate" -> Option(DateTime.now),
        "answers" -> Json.arr(
            Json.obj(
              "id" -> 1,
              "text" -> "cats are cooler",
              "s3ImageId" -> 0,
              "score" -> 0
            ), Json.obj(
              "id" ->2,
              "text" ->"dogs are cooler",
              "s3ImageId" -> 0,
              "score" -> 0
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

  //TODO: send 200 plus poll data when calling GET /polls/:id of hashId inserted above
}
