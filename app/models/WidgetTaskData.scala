package models

import java.util.UUID

import play.api.libs.json.Json

/**  Task data send to widget.
  */
case class WidgetTaskData(
                           verifier_id: UUID,
                           link: Link,
                           task: Task,
                           template: String
                  )

object WidgetTaskData {
  implicit val evalDataFormat = Json.format[WidgetTaskData]
}