package models

import java.util.UUID

import play.api.libs.json.Json

/** Tracks the verifications for a tasks.
  *
  * @param task_id UUID of the task for which the stats are collected
  * @param numTrue times a user verified the task as true
  * @param numFalse times a user verified the task as false
  * @param numNoValue times a user did not verify the task
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

