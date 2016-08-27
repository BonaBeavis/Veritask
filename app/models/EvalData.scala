package models

import java.util.UUID

import config.ConfigBanana
import play.api.libs.json.Json

import scala.language.implicitConversions

/**
  * Created by beavis on 18.08.16.
  */
case class EvalData(
                     _id: UUID,
                     user_id: UUID,
                     group: Int, //0=NoTasks,1=timed,2=ability
                     taskDelay: Long,
                     timeStamps: List[Long],
                     timePlayed: Long = 0L
                   ) extends MongoEntity

object EvalData extends ConfigBanana {
  implicit val evalDataFormat = Json.format[EvalData]
}
