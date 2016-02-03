package models

import java.util.UUID

import config.ConfigBanana
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.extensions.json.dao.JsonDao

case class Task(_id: UUID,
                taskset: String,
                s: String, p: String, o: String)

object Task extends ConfigBanana {
  import ops._
  import recordBinder._

  val clazz = URI("http://example.com/Task#class")
  implicit val classUris = classUrisFor[Task](clazz)
  val _id = property[String](vt("_id"))
  val taskset = property[String](vt("taskset"))
  val s = property[String](rdf.subject)
  val p = property[String](rdf.predicate)
  val o = property[String](rdf.obj)
  implicit val container = URI("http://example.com/persons/")
  implicit val binder = pgbWithId[Task](t => container.withFragment(t._id.toString))
    .apply(_id, taskset, s, p, o)(Task.apply, Task.unapply)
  implicit val userFormat = Json.format[Task]

  //  def create(triple: Rdf#Triple): Task = {
  //    val (s, p, o) = fromTriple(triple)
  //    val task = Task(None, s.toString, p.toString, o.toString)
  //    TaskMongoIdentity$.set(task, TaskMongoIdentity$.generateID(task))
  //  }

}

import play.api.Play.current

object TaskDao extends {
  override val autoIndexes = Seq(
    Index(Seq(
      "taskset" -> IndexType.Text,
      "s" -> IndexType.Text,
      "p" -> IndexType.Text
    ), unique = true, background = true)
  )
} with JsonDao[Task, String](current.injector.instanceOf[ReactiveMongoApi].db, "tasks")

