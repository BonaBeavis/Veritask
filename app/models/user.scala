package models

import play.api.libs.json.Json

case class User( age: Int,
                 firstName: String,
                 lastName: String,
                 active: Boolean)

object User {

  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val userFormat = Json.format[User]
  implicit val userWrite = Json.writes[User]
}
