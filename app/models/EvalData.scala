package models

import java.util.UUID

import config.ConfigBanana
import play.api.libs.json.Json

import scala.language.implicitConversions

/** Data collected about user for thesis's evaluation
  *
  * @param group to which evaluation group the user belongs
  *              0: user won't get tasks to verify
  *              1: user solves every [[taskDelay]] seconds a task
  *              2: users solves a task to be able to use an ability in the
  *                 MonsterMinigame
  * @param taskDelay Defines the time between tasks, if user's group is set to 1
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
