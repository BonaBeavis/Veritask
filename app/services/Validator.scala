package services


import com.google.inject.Inject
import models.{SimpleValidatorStats, Task, Verification}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by beavis on 08.06.16.
  */
trait Validator {
  def validate(verification: Verification): Future[Boolean]
}

class SimpleValidator @Inject()(
                                 val simpleValidatorStatsRepo: SimpleValidatorStatsRepo
                               ) extends Validator {

  def validate(verification: Verification): Future[Boolean] = {
    updateStats(verification).map(validation(_))
  }

  def updateStats(verification: Verification): Future[SimpleValidatorStats] = {
    val stats = simpleValidatorStatsRepo.search("task_id", verification.task_id.toString) flatMap {
      case s: Traversable[SimpleValidatorStats] if s.size == 1 => Future.successful(s.head)
      case s: Traversable[SimpleValidatorStats] if s.isEmpty =>
        Future.successful(new SimpleValidatorStats(task_id = verification.task_id))
      case _ => Future.failed(new Throwable("Database corrupt"))
    }
    val updatedStats = verification.value match {
      case None => stats.map(x => x.copy(numNoValue = x.numNoValue + 1))
      case Some(true) => stats.map(x => x.copy(numTrue = x.numTrue + 1))
      case Some(false) => stats.map(x => x.copy(numFalse = x.numFalse + 1))
    }
    for {
      updatedStats <- updatedStats
      savedStats <- simpleValidatorStatsRepo.save(updatedStats)
    } yield updatedStats
  }

  def validation(stats: SimpleValidatorStats) = stats.numTrue >= stats.numFalse
}
