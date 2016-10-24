package models

import java.util.UUID

import play.api.libs.json.Json

/**
  * Created by beavis on 18.08.16.
  */
case class WidgetTaskData(
                  verifier: UUID,
                  link: Link,
                  task: Task,
                  template: String
                  )

object WidgetTaskData {
  implicit val evalDataFormat = Json.format[WidgetTaskData]
}