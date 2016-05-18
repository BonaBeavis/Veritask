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
                    subjectsTarget: String,
                    linkPredicate: String,
                    objectsTarget: String,
                    subjectEndpoint: String,
                    objectEndpoint: String,
                    subjectAttributeQueries: Option[List[String]],
                    objectAttributeQueries: Option[List[String]]
                  ) extends MongoEntity

object Taskset {

  implicit val userFormat = Json.format[Taskset]

  val tasksetForm = Form(
    mapping(
      "_id" -> default(uuid, UUID.randomUUID()),
      "name" -> text,
      "subjectsTarget" -> text,
      "linkPredicate" -> text,
      "objectsTarget" -> text,
      "subjectEndpoint" -> text,
      "objectEndpoint" -> text,
      "subjectAttributeQueries" -> optional(list(text)),
      "objectAttributeQueries" -> optional(list(text))
    )(Taskset.apply)(Taskset.unapply)
  )
}

