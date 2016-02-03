package config

import org.w3.banana._
import org.w3.banana.sesame._

/**
  * Created by beavis on 02.12.15.
  */
trait ConfigBanana
  extends SesameModule {
  val foaf = FOAFPrefix[Rdf]
  val cert = CertPrefix[Rdf]
  val vt = VTPrefix[Rdf]
  val rdf = RDFPrefix[Rdf]
  val void = VoIDPrefix[Rdf]
}

object VTPrefix {
  def apply[Rdf <: RDF](implicit ops: RDFOps[Rdf]) = new VTPrefix()
}

class VTPrefix[Rdf <: RDF](implicit ops: RDFOps[Rdf])
  extends PrefixBuilder("vt", "http://aksw/veritask#")(ops) {
  val inTaskset = apply("inTaskset")
  val Tasks = apply("Tasks")
  val Verifier = apply("Verifier")
  val Result = apply("Result")
}

object VoIDPrefix {
  def apply[Rdf <: RDF](implicit ops: RDFOps[Rdf]) = new VoIDPrefix()
}

class VoIDPrefix[Rdf <: RDF](implicit ops: RDFOps[Rdf])
  extends PrefixBuilder("void", "http://rdfs.org/ns/void#")(ops) {
  val inTaskset = apply("inTaskset")
}
