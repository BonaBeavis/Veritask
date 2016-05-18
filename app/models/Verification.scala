package models

import java.util.UUID

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi

/**
  * Created by beavis on 03.12.15.
  */
case class Verification(_id: UUID,
                        verifier: UUID,
                        task: Task,
                        result: Boolean)

object Verification {
  implicit val verificationFormat = Json.format[Verification]
}

import play.api.Play.current

