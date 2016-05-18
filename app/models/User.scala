package models

import java.util.UUID

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi

case class User(_id: UUID)

object User {
  implicit val userFormat = Json.format[User]
}

import play.api.Play.current


