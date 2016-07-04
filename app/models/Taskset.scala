package models

import java.util.UUID

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

case class Taskset(
                    _id: UUID,
                    name: String,
                    subjectEndpoint: Option[String],
                    objectEndpoint: Option[String],
                    subjectAttributesQuery: Option[String],
                    objectAttributesQuery: Option[String],
                    template: String
                  ) extends MongoEntity

object Taskset {

  implicit val userFormat = Json.format[Taskset]

  val tasksetForm = Form(
    mapping(
      "_id" -> default(uuid, UUID.randomUUID()),
      "name" -> text,
      "subjectEndpoint" -> optional(text),
      "objectEndpoint" -> optional(text),
      "subjectAttributesQuery" -> optional(text),
      "objectAttributesQuery" -> optional(text),
      "template" -> text
    )(Taskset.apply)(Taskset.unapply)
  )
}

