package services


import com.google.inject.Inject
import models.{SimpleValidatorStats, Task, Verification}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.commons.math3.stat.interval.WilsonScoreInterval
import org.apache.commons.math3.stat.interval.ConfidenceInterval
/**
  * Created by beavis on 08.06.16.
  */
trait Validator {
  def process(verification: Verification): Future[Double]
}

class SimpleValidator @Inject()(
                                 val simpleValidatorStatsRepo: SimpleValidatorStatsRepo,
                                 val configuration: play.api.Configuration
                               ) extends Validator {

  override def process(verification: Verification): Future[Double] = {
    updateStats(verification).map(wilsonScoreCenter(_))
  }

  def updateStats(verification: Verification): Future[SimpleValidatorStats] = {
    val stats = simpleValidatorStatsRepo.getStats(verification) map {
      case Some(s) => s
      case None => new SimpleValidatorStats(task_id = verification.task_id)
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

  def wilsonScoreCenter(stats: SimpleValidatorStats) = {
    val confidence = configuration.getDouble("veritask.confidence").get
    def wilsonScoreInterval = new WilsonScoreInterval
    def interval = wilsonScoreInterval.createInterval(
      stats.numTrue + stats.numFalse, stats.numTrue, confidence)
    (interval.getLowerBound + interval.getUpperBound) / 2
  }
}
