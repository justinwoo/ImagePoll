package models


import com.github.nscala_time.time.Imports._
import reactivemongo.bson._

case class Vote (
  pollId: String,
  userId: String,
  answerIdsToIncrement: List[Int],
  voteTime: DateTime
)

object JsonFormat {
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  implicit val voteFormat = Json.format[Vote]
}
