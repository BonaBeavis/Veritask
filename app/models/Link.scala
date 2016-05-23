package models

import java.util.UUID

import play.api.libs.json.Json

case class Link(_id: UUID,
                linkSubject: String,
                predicate: String,
                linkObject: String
               ) extends MongoEntity

object Link {
  implicit val userFormat = Json.format[Link]
}

