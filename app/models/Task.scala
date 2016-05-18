package models

import java.util.UUID

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}

case class Task(_id: UUID,
                taskset: UUID,
                link: Link
               )

object Task {
  implicit val userFormat = Json.format[Task]
}

import play.api.Play.current

object TaskDao extends {
//  override val autoIndexes = Seq(
//    Index(Seq(
//      "taskset" -> IndexType.Text
//    ), unique = true, background = true)
//  )
} //with JsonDao[Task, UUID](current.injector.instanceOf[ReactiveMongoApi].db, "tasks")

