package models

import play.api.libs.json.Json

/**
  * Created by beavis on 06.06.16.
  */
case class Validation(
                     task_id: String,
                     value: Boolean
                     )
object Validation {
  implicit val validationFormat = Json.format[Validation]
}