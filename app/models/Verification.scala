package models

import ops._
import play.api.libs.json.Json
import recordBinder._

/**
  * Created by beavis on 03.12.15.
  */
case class Verification(verifier: Person, result: Boolean) {
}

object Verification {

  val clazz = URI("http://example.com/Verification#class")
  implicit val classUris = classUrisFor[Verification](clazz)

  val verifier = property[Person](vt.Verifier)
  val result = property[Boolean](vt.Result)

  implicit val container = URI("http://example.com/Verification/")
  implicit val binder =
    pgb[Verification](verifier, result)(Verification.apply, Verification.unapply)
  implicit val verificationFormat = Json.format[Task]
}
