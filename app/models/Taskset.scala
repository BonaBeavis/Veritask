package models

import java.util.UUID

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.extensions.json.dao.JsonDao

case class Taskset(
                    _id: UUID,
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
      "_id" -> default(uuid, UUID.randomUUID()),
      "name" -> text,
      "subjectsTarget" -> text,
      "linkPredicate" -> text,
      "objectsTarget" -> text,
      "subjectEndpoint" -> text,
      "objectEndpoint" -> text
    )(Taskset.apply)(Taskset.unapply)
  )
}

import play.api.Play.current

object TasksetDao extends JsonDao[Taskset, UUID](current.injector.instanceOf[ReactiveMongoApi].db, "tasksets") {
  def findByIdString(idString: String) = findById(UUID.fromString(idString))
}


