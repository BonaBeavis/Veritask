package models

import ops._
import org.w3.banana.binder.PGBinder
import play.api.libs.json.Json
import recordBinder._

/**
  * Created by beavis on 02.12.15.
  */
case class Taskset(uuid: Option[String], tasks: Set[Task] = Set.empty) {
}

object Taskset {
  val clazz = URI("http://example.com/Taskset#class")
  implicit val classUris = classUrisFor[Taskset](clazz)

  val uuid = property[Option[String]](vt("id"))
  val tasks = set[Task](vt.Tasks)

  //ToDo: Find out for what container is
  implicit val container = URI("http://example.com/tasksets")
  implicit val binder: PGBinder[Rdf, Taskset] =
    pgbWithId[Taskset](t => URI("http://example.com/tasksets" + t.uuid))
      .apply(uuid, tasks)(Taskset.apply, Taskset.unapply) withClasses classUris

  implicit val userFormat = Json.format[Taskset]

  implicit object VesselIdentity extends Identity[Taskset, String] {
    val name = "uuid"

    def of(entity: Taskset): Option[String] = entity.uuid

    def set(entity: Taskset, id: String): Taskset = entity.copy(uuid = Option(id))

    def clear(entity: Taskset): Taskset = entity.copy(uuid = None)

    def next: String = "Placeholder"
  }

}
