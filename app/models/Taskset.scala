package models

import java.util.UUID

import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json

case class Taskset(
                    uuid: Option[UUID],
                    name: String,
                    subjectsTarget: String,
                    linkPredicate: String,
                    objectsTarget: String,
                    subjectEndpoint: String,
                    objectEndpoint: String
                  )

object Taskset {

  implicit val userFormat = Json.format[Taskset]

  implicit val tasksetForm = Form(
    mapping(
      "uuid" -> optional(uuid),
      "name" -> text,
      "subjectsTarget" -> text,
      "linkPredicate" -> text,
      "objectsTarget" -> text,
      "subjectEndpoint" -> text,
      "objectEndpoint" -> text
    )(Taskset.apply)(Taskset.unapply)
  )

  implicit object TasksetIdentity extends Identity[Taskset, UUID] {

    val name = "uuid"

    def of(entity: Taskset): Option[UUID] = entity.uuid

    def set(entity: Taskset, id: UUID): Taskset = entity.copy(uuid = Option(id))

    def generateID(entity: Taskset): UUID = UUID.randomUUID()
  }

}
