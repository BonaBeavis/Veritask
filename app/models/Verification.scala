package models

import java.util.UUID

import config.ConfigBanana
import org.w3.banana.{FOAFPrefix, PointedGraph, XSDPrefix}
import org.w3.banana.binder.PGBinder
import play.api.libs.json.Json

import scala.language.implicitConversions
import scala.util.Try

case class Verification(_id: UUID,
                        verifier: UUID,
                        task_id: UUID,
                        value: Option[Boolean]
                       ) extends MongoEntity

object Verification extends ConfigBanana {
  implicit val verificationFormat = Json.format[Verification]
}

case class VerificationDump(
                          _id: UUID,
                          verifier: String,
                          link: Link,
                          value: Option[Boolean]
                           )

object VerificationDump extends ConfigBanana {

    import ops._
    import recordBinder._
    import org.w3.banana.syntax._

    val clazz = URI("http://example.com/City#class")
    implicit val classUris = classUrisFor[VerificationDump](clazz)

    val _id = property[UUID](foaf("id"))
    val verifier = property[String](URI("http://purl.org/dc/terms/contributor"))
    val link= property[Link](foaf("link"))
    val value = optional[Boolean](foaf("value"))

    implicit val binder: PGBinder[Rdf, VerificationDump] =
      pgbWithId[VerificationDump](t => URI("http://example.com/" + t._id))
        .apply(_id, verifier, link, value)(VerificationDump.apply, VerificationDump.unapply) withClasses classUris
}
