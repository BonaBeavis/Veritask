package models

import java.util.UUID

import play.api.libs.json.Json

/**
  * Created by beavis on 18.08.16.
  */
case class EvalData(
                 _id: UUID,
                 user_id: UUID,
                 group: Int,
                 timeStamps: List[Long]
) extends MongoEntity

object EvalData {
  implicit val evalDataFormat = Json.format[EvalData]
}
