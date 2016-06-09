package models

import java.util.UUID

import play.api.libs.json.Json

/**
  * Created by beavis on 08.06.16.
  */
case class SimpleValidatorStats(
                               _id: UUID = UUID.randomUUID(),
                               task_id: UUID,
                               numTrue: Int = 0,
                               numFalse: Int = 0,
                               numNoValue: Int = 0
                               ) extends MongoEntity

object SimpleValidatorStats {
  implicit val verificationFormat = Json.format[SimpleValidatorStats]
}

