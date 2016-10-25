package services

import java.util.UUID

import models._

import scala.concurrent.{ExecutionContext, Future}

/** Interface for persistence layer. The following three traits are helper
  * functions for specific repositories.
  */
trait Repository[E] {

  def save(entity: E)(implicit ec: ExecutionContext): Future[E]

  /** Persists huge number of entities.
    *
    * @return number of entities saved
    */
  def bulkSave(entities: Seq[E])(implicit ec: ExecutionContext): Future[Int]

  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]]

  def search(key: String, value: String)
    (implicit ec: ExecutionContext): Future[Traversable[E]]

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]]

  def delete(id: UUID)(implicit ec: ExecutionContext): Future[Option[UUID]]
}

trait TaskRepository extends Repository[Task] {
  /** Select the next task to verify.
    *
    * @param taskset select a task from this taskset
    * @param excludedTasks a list of task already verified by the user
    *
    * TODO: Should be a service
    */
  def selectTaskToVerify(taskset: Option[String], excludedTasks: List[UUID] = List())
    (implicit ec: ExecutionContext): Future[Option[Task]]
}

trait VerificationRepository extends Repository[Verification] {
  def getLink(verification: Verification)
    (implicit ec: ExecutionContext): Future[Link]
}

trait SimpleValidatorStatsRepository extends Repository[SimpleValidatorStats] {
  def getStats(verification: Verification)
    (implicit ec: ExecutionContext): Future[Option[SimpleValidatorStats]]
}
