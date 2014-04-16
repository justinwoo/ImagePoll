package models

import com.github.nscala_time.time.Imports._
import reactivemongo.bson._


case class Answer (
  id: Int, 
  text: String,
  s3ImageId: Long
)

case class VotingSystem (
  votingType: String,
  n: Int
)

case class Poll (
  hashId: String,
  title: String,
  questionText: String,
  expirationDate: Option[DateTime],
  createdDate: Option[DateTime],
  answers: List[Answer],
  votingSystem: VotingSystem
)

object JsonFormats {
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  implicit val answerFormat = Json.format[Answer]
  implicit val votingSystemFormat = Json.format[VotingSystem]
  implicit val pollFormat = Json.format[Poll]
  implicit val voteFormat = Json.format[Vote]
  
}
