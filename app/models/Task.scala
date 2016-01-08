package models

import java.util.UUID

import config.ConfigBanana
import play.api.libs.json.Json

/**
  * Created by beavis on 03.12.15.
  */
case class Task(_id: Option[String], s: String, p: String, o: String) {
}

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
  implicit val binder = pgb[Task](_id, s, p, o)(Task.apply, Task.unapply)
  implicit val userFormat = Json.format[Task]

  def create(triple: Rdf#Triple): Task = {
    val (s, p, o) = fromTriple(triple)
    Task(Option("String"), s.toString, p.toString, o.toString)
  }

  implicit object TaskIdentity extends Identity[Task, String] {
    val name = "_id"

    def of(entity: Task): Option[String] = entity._id

    def set(entity: Task, id: String): Task = entity.copy(_id = Option(id))

    def clear(entity: Task): Task = entity.copy(_id = None)

    def next: String = UUID.randomUUID().toString
  } 
}
