package models

import java.util.UUID

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}

case class Task(_id: UUID,
                taskset_id: UUID,
                link_id: UUID,
                subjectAttributes: Option[Map[String, String]],
                objectAttributes: Option[Map[String, String]]
               ) extends MongoEntity

object Task {
  implicit val taskFormat = Json.format[Task]
}

