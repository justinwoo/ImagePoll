package models


import com.github.nscala_time.time.Imports._
import reactivemongo.bson._

case class Vote (
  pollId: String,
  userId: String,
  answerIdsToIncrement: List[Int],
  voteTime: DateTime
)
