package config

import org.w3.banana._
import org.w3.banana.binder.RecordBinder
import org.w3.banana.io._
import org.w3.banana.sesame._
import org.w3.banana.sesame.io._

import scala.util.Try

/**
  * Created by beavis on 02.12.15.
  */
trait ConfigBanana {
  type Rdf = Sesame
  implicit val ops: RDFOps[Rdf] = Sesame.ops
  implicit val recordBinder: RecordBinder[Rdf] = Sesame.recordBinder
  implicit val sesameRDFWriterHelper = new SesameRDFWriterHelper
  implicit val jsonldReader: SesameJSONLDReader = new SesameJSONLDReader
  implicit val jsonldWriter: RDFWriter[Rdf, Try, JsonLdFlattened] = sesameRDFWriterHelper.jsonldFlattenedWriter
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
}

object VoIDPrefix {
  def apply[Rdf <: RDF](implicit ops: RDFOps[Rdf]) = new VoIDPrefix()
}

class VoIDPrefix[Rdf <: RDF](implicit ops: RDFOps[Rdf])
  extends PrefixBuilder("void", "http://rdfs.org/ns/void#")(ops) {
  val inTaskset = apply("inTaskset")
}
