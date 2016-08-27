package controllers

import java.util.UUID
import javax.inject.Inject

import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services._

import scala.concurrent.Future

/**
  * Created by beavis on 26.08.16.
  */
class Evaluation @Inject()(
                            val evalDataRepo: EvalDataRepo,
                            val verificationRepo: VerificationMongoRepo,
                            val messagesApi: MessagesApi
                          )
  extends Controller with I18nSupport {

  case class EvalPlaytime(user: UUID, group: Int, timePlayed: Long)
  implicit val evalFormat = Json.format[EvalPlaytime]

  def playTime = Action.async {
    evalDataRepo.findAll.map(_.map(
      evalData => EvalPlaytime(
        evalData.user_id, evalData.group, evalData.timePlayed
      )
    )).map(r => Ok(Json.toJson(r)))
  }

  def verifications = Action.async {
    verificationRepo.findAll.map(t => Ok(Json.toJson(t)))
  }

//  def calcTimePlayed(evalData: EvalData, window: Long): Long = {
//    evalData.timeStamps.sliding(2) map { slide =>
//      if (interval > window) interval else 0L
//    }
//  }.sum
}