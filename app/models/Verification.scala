package models

import play.api.libs.json.Json

/**
  * Created by beavis on 03.12.15.
  */
case class Verification(verifier: String, result: Boolean) {
}

object Verification {

  implicit val verificationFormat = Json.format[Task]
}
