package models

import java.util.UUID

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/** Stores metadata common to tasks generated from a linkset.
  *
  * @param name a name for the taskset
  * @param subjectEndpoint a sparql endpoint to query the attributes for the
  *                        subject of a link
  * @param objectEndpoint a sparql endpoint to query the attributes for the
  *                        object of a link
  * @param subjectAttributesQuery a sparql SELECT query, where before executing
  *                               the query, {{ linkSubjectURI }} will be
  *                               substituted with the URI of the link subject.
  * @param objectAttributesQuery analog to subjectAttributesQuery
  * @param template a jsrender html template for presenting a task
  */
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

  // Defines constraints for the taskset form
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

