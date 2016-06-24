package models

import java.util.UUID

import play.api.libs.json.Json


case class User(
                 _id: UUID,
                 name: String,
                 group: Int,
                 timeStamps: List[Long]
) extends MongoEntity

object User {
  implicit val userFormat = Json.format[User]
}

