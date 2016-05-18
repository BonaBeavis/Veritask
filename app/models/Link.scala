package models

import play.api.libs.json.Json

case class Link(linkSubject: String,
                predicate: String,
                linkObject: String
               )

object Link {
  implicit val userFormat = Json.format[Link]
}

