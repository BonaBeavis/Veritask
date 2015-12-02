package config

import org.w3.banana._
import org.w3.banana.binder.RecordBinder
import org.w3.banana.jena.Jena

/**
  * Created by beavis on 02.12.15.
  */
trait ConfigBanana {
  type Rdf = Jena
  implicit val ops: RDFOps[Rdf] = Jena.ops
  implicit val recordBinder: RecordBinder[Rdf] = Jena.recordBinder
  val foaf = FOAFPrefix[Rdf]
  val cert = CertPrefix[Rdf]
  val vt = VTPrefix[Rdf]
}

object VTPrefix {
  def apply[Rdf <: RDF](implicit ops: RDFOps[Rdf]) = new VTPrefix()
}

class VTPrefix[Rdf <: RDF](implicit ops: RDFOps[Rdf])
  extends PrefixBuilder("vt", "http://aksw/veritask#")(ops) {
  val task = apply("Taskset")
  val creator = apply("creator")
  // and so on...
}
