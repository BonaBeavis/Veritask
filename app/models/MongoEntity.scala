package models

import java.util.UUID

import play.api.libs.json.Json

/**
  * Created by beavis on 18.05.16.
  */
abstract class MongoEntity {
  val _id: UUID
  implicit val format = Json.format[Taskset]
}
