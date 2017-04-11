package services

import com.google.inject.Inject
import models.{SimpleValidatorStats, Validation, Verification}
import org.apache.commons.math3.stat.interval.WilsonScoreInterval

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/** Validates a verification. To give feedback to the widget if the user
  * verified "right". It is not known if a link is correct, so the correctness
  * must be estimated.
  */
trait Validator {
  def validate(verification: Verification): Future[Validation]
}

/** Implements a estimation based on a wilson score.
  * Estimation is based on the the agreements of all users.
  *
  * If the lower bound of the wilson score is higher than 0.5, we can assume
  * with a confidence = (intervalconfidence + 1)/2 that the majority of users
  * think the link is true.
  *
  * Analog with false
  *
  * If the sample is too small to reach either condition, the verification is
  * validated as true. We dont want to punish the user, just because he verified
  * a new link.
  */
class SimpleValidator @Inject()(
                                 val simpleValidatorStatsRepo: SimpleValidatorStatsMongoRepo,
                                 val configuration: play.api.Configuration
                               ) extends Validator {
  /** @return True if not enough verifications for a estimation exists.
    *
    */
  override def validate(verification: Verification): Future[Validation] = {
    updateStats(verification) map {
      stats => new Validation(
        verification.task_id,
        System.currentTimeMillis(),
        (wilsonEstimation(stats), verification.value) match {
          // Not enough verifications and user gave a value => gift true
          case (None, Some(v)) => Some(true)
          // If enough verifications for a estimation exist and user gave a value
          case (Some(wE), Some(v)) => Some(wE == v)
          // No verification, no validation
          case (_, None) => None
        }
      )
    }
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

  def wilsonEstimation(stats: SimpleValidatorStats, confidence: Option[Double] = None): Option[Boolean] = {
    val confi = confidence.getOrElse(configuration.getDouble("veritask.confidence").get)
    if (stats.numTrue + stats.numFalse == 0) {
      None
    } else {
      val wilsonScoreInterval = new WilsonScoreInterval
      val interval = wilsonScoreInterval.createInterval(
        stats.numTrue + stats.numFalse, stats.numTrue, confi)

      if (interval.getLowerBound > 0.5) {
        Some(true)
      }
      else if (interval.getUpperBound < 0.5) {
        Some(false)
      }
      else {
        None
      }
  } }
}
