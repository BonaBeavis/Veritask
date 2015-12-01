package models

import org.w3.banana.{RDFOps, _}
import org.w3.banana.binder.RecordBinder
import org.w3.banana.jena._
import play.api.libs.json.Json

/**
  * Created by beavis on 27.11.15.
  */
abstract class RDFPerson[Rdf <: RDF](implicit
                                     ops: RDFOps[Rdf],
                                     recordBinder: RecordBinder[Rdf]
                                    ) {

  import ops._
  import recordBinder._

  val foaf = FOAFPrefix[Rdf]
  val cert = CertPrefix[Rdf]

  val clazz = URI("http://example.com/Person#class")
  implicit val classUris = classUrisFor[Person](clazz)

  val name = property[String](foaf.name)
  val nickname = optional[String](foaf("nickname"))

  implicit val container = URI("http://example.com/persons/")
  implicit val binder = pgb[Person](name, nickname)(Person.apply, Person.unapply)
  implicit val userFormat = Json.format[Person]
}

case class Person(name: String, nickname: Option[String] = None)

object Person extends RDFPerson[Jena] {
}
