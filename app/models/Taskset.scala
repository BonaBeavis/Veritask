package models

import java.util.UUID

import play.api.libs.json.Json

case class Taskset(
                      uuid: Option[UUID],
                      subjectsTarget: Option[String],
                      linkPredicate: Option[String],
                      objectsTarget: Option[String],
                      subjectEndpoint: Option[String],
                      objectEndpoint: Option[String]
                  )

object Taskset {

  implicit val userFormat = Json.format[Taskset]

  implicit object TasksetIdentity extends Identity[Taskset, UUID] {

    val name = "uuid"

    def of(entity: Taskset): Option[UUID] = entity.uuid

    def set(entity: Taskset, id: UUID): Taskset = entity.copy(uuid = Option(id))

    def generateID(entity: Taskset): UUID = UUID.randomUUID()
  }

}
