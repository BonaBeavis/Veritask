package models

import java.util.UUID

import play.api.libs.json.Json

/**
  * Created by beavis on 03.12.15.
  */
case class Verification(_id: UUID,
                        verifier: UUID,
                        task_id: UUID,
                        value: Option[Boolean]
                       ) extends MongoEntity

object Verification {
  implicit val verificationFormat = Json.format[Verification]
}

