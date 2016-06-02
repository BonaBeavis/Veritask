package models

import java.util.UUID

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi

case class Taskset(
                    _id: UUID,
                    name: String,
                    subjectEndpoint: String,
                    objectEndpoint: String,
                    subjectAttributesQuery: String,
                    objectAttributesQuery: String,
                    template: String
                  ) extends MongoEntity

object Taskset {

  implicit val userFormat = Json.format[Taskset]

  val tasksetForm = Form(
    mapping(
      "_id" -> default(uuid, UUID.randomUUID()),
      "name" -> text,
      "subjectEndpoint" -> text,
      "objectEndpoint" -> text,
      "subjectAttributesQuery" -> text,
      "objectAttributesQuery" -> text,
      "template" -> text
    )(Taskset.apply)(Taskset.unapply)
  )
}

