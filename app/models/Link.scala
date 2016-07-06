package models

import config.ConfigBanana

import java.util.UUID
import org.w3.banana.binder.PGBinder
import play.api.libs.json.Json

case class Link(_id: UUID,
                linkSubject: String,
                predicate: String,
                linkObject: String,
                estimation: Option[Double]
               ) extends MongoEntity

object Link extends ConfigBanana {

  implicit val userFormat = Json.format[Link]

  import ops._
  import recordBinder._

  val _id = property[UUID](foaf("id"))
  val linkSubject = property[String](foaf("linkSubject"))
  val predicate = property[String](foaf("predicate"))
  val linkObject = property[String](foaf("linkObject"))
  val estimation = optional[Double](foaf("estimation"))

  implicit val binder: PGBinder[Rdf, Link] =
    pgbWithId[Link](t => URI("http://example.com/" + t._id))
        .apply(_id, linkSubject, predicate, linkObject, estimation)(Link.apply, Link.unapply)
}

