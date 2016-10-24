package config

import java.util.UUID

import org.w3.banana._
import org.w3.banana.binder.PGBinder
import org.w3.banana.jena._

import scala.util.Try

/** Provides the configuration for banana-rdf and implicits for RDF bindings.
  *
  * Classes handling RDF mixin this trait.
  * By changing the Module from which this trait extends. The RDF implementation
  * can be changed.
  */
trait ConfigBanana
  extends JenaModule {
  val foaf = FOAFPrefix[Rdf]
  val cert = CertPrefix[Rdf]
  val rdf = RDFPrefix[Rdf]

  import ops._

  /** Provides a RDF-binding for UUIDs.
    *
    * TODO: Mapping RDF-node to MongoID to scala case class _id attribute.
    */
  implicit val binderUUID: PGBinder[Rdf, UUID] = new PGBinder[Rdf, UUID] {
    def fromPG(pointed: PointedGraph[Rdf]): Try[UUID] = Try(UUID.randomUUID())

    def toPG(uuid: UUID): PointedGraph[Rdf] =
      bnode -- foaf.name ->- uuid.toString
  }
}


