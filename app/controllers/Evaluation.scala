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

case class UserEvalData(
                         user: UUID,
                         group: Int,
                         taskDelay: Long,
                         timePlayed: Long,
                         numVeriTrue: Option[Int],
                         numVeriFalse: Option[Int],
                         numVeriUnsure: Option[Int]
                       )

case class VeriEvalData(
                         veri: UUID,
                         value: Option[Boolean],
                         task: UUID,
                         group: Int,
                         gold: Boolean
                       )

// Task, Agreement, Gruppe, Verification(Gold)
case class TaskEvalData(
                         task: UUID,
                         taskset: UUID,
                         agreement: Option[Boolean],
                         group: Int,
                         correct: Option[Boolean]
                       )
/** Provides helper function for the evaluation.
  */
class Evaluation @Inject()(
                            val evalDataRepo: EvalDataRepo,
                            val verificationRepo: VerificationMongoRepo,
                            val valiStatsRepo: SimpleValidatorStatsMongoRepo,
                            val tasksetRepo: TasksetMongoRepo,
                            val taskRepo: TaskMongoRepo,
                            val validator: SimpleValidator,
                            val messagesApi: MessagesApi
                          )
  extends Controller with I18nSupport {


  implicit val userEvalDataFormat = Json.format[UserEvalData]
  implicit val veriEvalDataFormat = Json.format[VeriEvalData]
  implicit val taskEvalDataFormat = Json.format[TaskEvalData]

  //Spieler, Spielzeit, Gruppe, Verification(None), Verification(True), Verification(False)
  def userEvalData = Action.async {
    val evalData = evalDataRepo.findAll
    val verifications = verificationRepo.findAll
    val data = for {
      evalData <- evalData
      verifications <- verifications
      veris = veriPerUser(verifications)
    } yield {
      for {
        playtime <- playTime(evalData)
        veri = veris.get(playtime.user)
      } yield {
        playtime.copy(
          numVeriTrue = veri.map(_.getOrElse(Some(true), 0)),
          numVeriFalse = veri.map(_.getOrElse(Some(false), 0)),
          numVeriUnsure = veri.map(_.getOrElse(None, 0))
        )
      }
    }
    data.map(d => Ok(Json.toJson(d)))
  }

  def playTime(evalDatas: Traversable[EvalData]): Traversable[UserEvalData] = {
    evalDatas.map(
      evalData => UserEvalData(
        evalData.user_id, evalData.group, evalData.taskDelay, evalData.timePlayed, None, None, None)
    )
  }

  def veriPerUser(verifications: Traversable[Verification]) = {
    val vPU = verifications.groupBy(_.verifier_id)
    vPU.mapValues(_.groupBy(_.value).mapValues(_.size))
  }

  //VeriID, Verification(value), Task, Gruppe, Verification(gold)
  def veriEvalData = Action.async {
    val evalDatas = evalDataRepo.findAll
    val verifications = verificationRepo.findAll
    val data = for {
      evalDatas <- evalDatas
      verifications <- verifications
    } yield {
      for {
        verifcation <- verifications
        evalData = evalDatas.find(_.user_id == verifcation.verifier_id).get
      } yield VeriEvalData(
        veri = verifcation._id,
        value = verifcation.value,
        task = verifcation.task_id,
        group = evalData.group,
        gold = true)
    }
    data.map(d => Ok(Json.toJson(d)))
  }

  def veriPerTask(verifications: Traversable[Verification]) = {
    val vPU = verifications.groupBy(_.task_id)
    vPU.mapValues(_.groupBy(_.value).mapValues(_.size))
  }
  // Task, Agreement, Gruppe, Verification(Gold)
  def taskEvalData(group: Int, confidence: Option[Double]) = Action.async {
    val verifications = verificationRepo.findAll
    val evalDatas = evalDataRepo.findAll
    val taskModels = taskRepo.findAll
    val tasksets = tasksetRepo.findAll
    val data = for {
      evalDatas <- evalDatas
      taskModels <- taskModels
      tasksets <- tasksets
      verifications <- verifications.map(_.filter(veri => evalDatas.find(_.user_id == veri.verifier_id).get.group == group))
      tasks = veriPerTask(verifications)
    } yield {
      for {
        task <- tasks
        taskset = tasksets.find(t => t._id == taskModels.find(x => x._id == task._1).get.taskset_id)
        stats = SimpleValidatorStats(
          task_id = task._1,
          numTrue = task._2.getOrElse(Some(true), 0),
          numFalse = task._2.getOrElse(Some(false), 0),
          numNoValue = task._2.getOrElse(None, 0)
        )
      } yield TaskEvalData(
        task = task._1,
        taskset = taskset.get._id,
        agreement = validator.wilsonEstimation(stats, confidence),
        group = group,
        correct = isCorrect(validator.wilsonEstimation(stats, confidence), taskset.get))
    }

    data.map(d => Ok(Json.toJson(d)))
  }

 def isCorrect(agreement: Option[Boolean], taskset: Taskset): Option[Boolean] = {
   val result = (agreement, taskset) match {
     case (Some(agreement), taskset: Taskset) if agreement != (taskset.name == "True") =>
       Option(false)
     case (Some(agreement), taskset: Taskset) if agreement == (taskset.name == "True") =>
       Option(true)
     case (None, taskset) => None
   }
   result
  }
}