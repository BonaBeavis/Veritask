package models

import java.util.UUID

import play.api.libs.json.Json

/** Entities to be persisted to MongoDB must extend from this class.
  */
abstract class MongoEntity {
  val _id: UUID
  implicit val format = Json.format[Taskset]
}
