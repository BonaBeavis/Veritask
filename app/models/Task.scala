package models

import java.util.UUID

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.extensions.json.dao.JsonDao

case class Task(_id: UUID,
                taskset: UUID,
                subjectURL: String,
                objectURL: String
               )

object Task {
  implicit val userFormat = Json.format[Task]
}

import play.api.Play.current

object TaskDao extends {
  override val autoIndexes = Seq(
    Index(Seq(
      "taskset" -> IndexType.Text,
      "subjectURL" -> IndexType.Text,
      "objectURL" -> IndexType.Text
    ), unique = true, background = true)
  )
} with JsonDao[Task, UUID](current.injector.instanceOf[ReactiveMongoApi].db, "tasks")

