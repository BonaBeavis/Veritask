package config

import java.net.URL

import org.w3.banana._
import org.w3.banana.jena._

import scala.concurrent.Future

/**
  * Created by beavis on 02.12.15.
  */
trait ConfigBanana
  extends JenaModule {
  val foaf = FOAFPrefix[Rdf]
  val cert = CertPrefix[Rdf]
  val rdf = RDFPrefix[Rdf]
}
