package models

import java.util.UUID

import config.ConfigBanana
import org.w3.banana.PointedGraph
import org.w3.banana.binder.PGBinder
import play.api.libs.json.Json

import scala.util.Try

case class Link(_id: UUID,
                linkSubject: String,
                predicate: String,
                linkObject: String
               ) extends MongoEntity

object Link extends ConfigBanana {
  implicit val userFormat = Json.format[Link]

  import ops._
  import recordBinder._
  import org.w3.banana.syntax._

  val _id = property[UUID](foaf("id"))
  val linkSubject = property[String](foaf("linkSubject"))
  val predicate = property[String](foaf("predicate"))
  val linkObject = property[String](foaf("linkObject"))



  implicit val binder: PGBinder[Rdf, Link] =
    pgbWithId[Link](t => URI("http://example.com/" + t._id))
        .apply(_id, linkSubject, predicate, linkObject)(Link.apply, Link.unapply)
}

