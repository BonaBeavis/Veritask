package models

import java.util.UUID

import config.ConfigBanana
import org.w3.banana.{FOAFPrefix, XSDPrefix}
import org.w3.banana.binder.PGBinder
import play.api.libs.json.Json

import scala.language.implicitConversions

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

//object UUID extends ConfigBanana{
//    import ops._
//    import recordBinder._
//    import org.w3.banana.syntax._
//
//    implicit val rsaClassUri = classUrisFor[RSAPublicKey](cert.RSAPublicKey)
//    val factory = KeyFactory.getInstance("RSA")
//    val exponent = property[BigInteger](cert.exponent)
//    val modulus = property[Array[Byte]](cert.modulus)
//
//    implicit val binder: PGBinder[Rdf, RSAPublicKey] =
//      pgb[RSAPublicKey](modulus, exponent)(
//        (m, e) => factory.generatePublic(new RSAPublicKeySpec(new BigInteger(m), e)).asInstanceOf[RSAPublicKey],
//        key => Some((key.getModulus.toByteArray, key.getPublicExponent))
//      ) // withClasses rsaClassUri
//
//  }
//  import ops._
//  import recordBinder._
//  val clazz = URI("http://example.com/Taskset#class")
//  implicit val classUris = classUrisFor[Verification](clazz)
//
//  val _id = property[Option[String]](vt("_id"))
//  val tasks = set[Task](vt.Tasks)
//
//  //ToDo: Find out for what container is
//  implicit val container = URI("http://example.com/tasksets")
//  implicit val binder: PGBinder[Rdf, Taskset] =
//    pgbWithId[Taskset](t => URI("http://example.com/tasksets" + t._id))
//      .apply(_id, tasks)(Taskset.apply, Taskset.unapply) withClasses classUris
//case class City(cityName: String, otherNames: Set[String] = Set.empty)
//
//object City {
//
//  val clazz = URI("http://example.com/City#class")
//  implicit val classUris = classUrisFor[City](clazz)
//
//  val cityName = property[String](foaf("cityName"))
//  val otherNames = set[String](foaf("otherNames"))
//
//  implicit val binder: PGBinder[Rdf, City] =
//    pgbWithId[City](t => URI("http://example.com/" + t.cityName))
//      .apply(cityName, otherNames)(City.apply, City.unapply) withClasses classUris
//
//}