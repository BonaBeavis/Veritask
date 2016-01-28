package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

case class Taskset(
                    _id: Option[String],
                    name: String,
                    subjectsTarget: String,
                    linkPredicate: String,
                    objectsTarget: String,
                    subjectEndpoint: String,
                    objectEndpoint: String
                  )

object Taskset {

  implicit val userFormat = Json.format[Taskset]

  val tasksetForm = Form(
    mapping(
      "_id" -> optional(text),
      "name" -> text,
      "subjectsTarget" -> text,
      "linkPredicate" -> text,
      "objectsTarget" -> text,
      "subjectEndpoint" -> text,
      "objectEndpoint" -> text
    )(Taskset.apply)(Taskset.unapply)
  )
}
