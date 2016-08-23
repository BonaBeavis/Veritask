package services

import java.util.UUID

import models._

import scala.concurrent.{ExecutionContext, Future}

trait Repository[E] {

  def save(entity: E)(implicit ec: ExecutionContext): Future[E]

  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]]

  def findById()(implicit ec: ExecutionContext): Future[Option[E]]

  def search(key: String, value: String)
    (implicit ec: ExecutionContext): Future[Traversable[E]]

  def search(key: String, values: Traversable[String])
    (implicit ec: ExecutionContext): Future[Traversable[E]]

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]]

  def delete(id: UUID)(implicit ec: ExecutionContext): Future[Option[UUID]]
}

trait TaskRepository extends Repository[Task] {
  def selectTaskToVerify(taskset: Option[String], excludedTasks: List[String] = List())
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
