package config

import java.net.URL
import java.util.UUID

import org.w3.banana._
import org.w3.banana.binder.{LiteralBinder, PGBinder}
import org.w3.banana.jena._

import scala.concurrent.Future
import scala.util.Try

/**
  * Created by beavis on 02.12.15.
  */
trait ConfigBanana
  extends JenaModule {
  val foaf = FOAFPrefix[Rdf]
  val cert = CertPrefix[Rdf]
  val rdf = RDFPrefix[Rdf]

  import ops._
  import recordBinder._
  implicit val binderUUID: PGBinder[Rdf, UUID] = new PGBinder[Rdf, UUID] {
    def fromPG(pointed: PointedGraph[Rdf]): Try[UUID] = Try(UUID.randomUUID())

    def toPG(uuid: UUID): PointedGraph[Rdf] =
      bnode -- foaf.name ->- uuid.toString
    xsd.long
  }


}


