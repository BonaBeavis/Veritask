package models

import config.ConfigBanana
import play.api.libs.json.Json

case class Task(_id: Option[String], s: String, p: String, o: String)

object Task extends ConfigBanana {

  import ops._
  import recordBinder._

  val clazz = URI("http://example.com/Task#class")
  implicit val classUris = classUrisFor[Task](clazz)
  val _id = optional[String](vt("_id"))
  val s = property[String](rdf.subject)
  val p = property[String](rdf.predicate)
  val o = property[String](rdf.obj)
  implicit val container = URI("http://example.com/persons/")
  implicit val binder = pgbWithId[Task](t => container.withFragment(t._id.get.toString))
      .apply(_id, s, p, o)(Task.apply, Task.unapply)
  implicit val userFormat = Json.format[Task]

  //  def create(triple: Rdf#Triple): Task = {
  //    val (s, p, o) = fromTriple(triple)
  //    val task = Task(None, s.toString, p.toString, o.toString)
  //    TaskMongoIdentity$.set(task, TaskMongoIdentity$.generateID(task))
  //  }

}
