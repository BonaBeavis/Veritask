package models

import ops._
import play.api.libs.json.Json
import recordBinder._

/**
  * Created by beavis on 03.12.15.
  */
case class Task(s: String, p: String, o: String) {
}

object Task {

  val clazz = URI("http://example.com/Task#class")
  implicit val classUris = classUrisFor[Task](clazz)

  val s = property[String](rdf.subject)
  val p = property[String](rdf.predicate)
  val o = property[String](rdf.obj)

  implicit val container = URI("http://example.com/persons/")
  implicit val binder = pgb[Task](s, p, o)(Task.apply, Task.unapply)
  implicit val userFormat = Json.format[Task]
}
