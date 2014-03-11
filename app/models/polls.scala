package models

import com.github.nscala_time.time.Imports._
import reactivemongo.bson._

// TODO: should i expand these into separate files?

case class Answer (
  id: Integer,
  text: String,
  s3ImageId: Long,
  score: Long
)

case class VotingSystem (
  votingType: String,
  n: Integer
)

case class Poll (
  id: Option[BSONObjectID],
  title: String,
  questionText: String,
  expirationDate: Option[DateTime],
  createdDate: Option[DateTime],
  answers: List[Answer],
  votingSystem: Option[VotingSystem]
)



object Poll {
  implicit object AnswerBSONReader extends BSONDocumentReader[Answer] {
    def read (document: BSONDocument) : Answer = {
      Answer (
        document.getAs[BSONInteger]("id").get.value,
        document.getAs[BSONString]("text").get.value,
        document.getAs[BSONLong]("s3ImageId").get.value,
        document.getAs[BSONLong]("score").get.value
      )
    }
  }

  implicit object AnswerBSONWriter extends BSONDocumentWriter[Answer] {
    def write (answer: Answer) = {
      BSONDocument (
        "id" -> BSONInteger(answer.id),
        "text" -> BSONString(answer.text),
        "s3ImageId" -> BSONLong(answer.s3ImageId),
        "score" -> BSONLong(answer.score)
      )
    }
  }

  implicit object VotingSystemBSONReader extends BSONDocumentReader[VotingSystem] {
    def read (document: BSONDocument) : VotingSystem = {
      VotingSystem (
        document.getAs[BSONString]("votingType").get.value,
        document.getAs[BSONInteger]("n").get.value
      )
    }  
  }

  implicit object VotingSystemBSONWriter extends BSONDocumentWriter[VotingSystem] {
    def write (system: VotingSystem) = {
      BSONDocument (
        "votingType" -> BSONString(system.votingType),
        "n" -> BSONInteger(system.n)
      )    
    }
  }

  implicit object PollBSONReader extends BSONDocumentReader[Poll] {
    def read (document: BSONDocument) : Poll = {
      Poll (
        document.getAs[BSONObjectID]("_id"),
        document.getAs[BSONString]("title").get.value,
        document.getAs[BSONString]("questionText").get.value,
        document.getAs[BSONDateTime]("expirationDate").map(date => new DateTime(date.value)),
        document.getAs[BSONDateTime]("createdDate").map(date => new DateTime(date.value)),
        document.getAs[BSONArray]("answers").get.values.map( a =>
          AnswerBSONReader.read(a.asInstanceOf[BSONDocument])
        ).toList,
        document.getAs[BSONArray]("votingSystem").map( votingSystem =>
          VotingSystemBSONReader.read(votingSystem.asInstanceOf[BSONDocument])
        )
      )
    }
  }

  implicit object PollBSONWriter extends BSONDocumentWriter[Poll] {
    def write (poll: Poll): BSONDocument = {
      BSONDocument (
        "_id" -> poll.id.getOrElse(BSONObjectID.generate),
        "title" -> BSONString(poll.title),
        "questionText" -> BSONString(poll.questionText),
        "expirationDate" -> BSONDateTime(poll.expirationDate.get.getMillis),
        "createdDate" -> BSONDateTime(poll.createdDate.get.getMillis),
        "answers" -> BSONArray(poll.answers.map( a => 
          AnswerBSONWriter.write(a))
        ),
        "votingSystem" -> BSONArray(poll.votingSystem.map ( v =>
          VotingSystemBSONWriter.write(v))
        )
      )  
    }  
  }
}
