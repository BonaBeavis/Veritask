package services

import java.util.UUID

import com.google.inject.Inject
import models._

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{MultiBulkWriteResult, WriteResult}

trait Repository[E] {

  def save(entity: E)(implicit ec: ExecutionContext): Future[E]

  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]]

  def findById()(implicit ec: ExecutionContext): Future[Option[E]]

  def search(key: String, value: String)(implicit ec: ExecutionContext): Future[Traversable[E]]

  def search(key: String, values: Traversable[String])(implicit ec: ExecutionContext): Future[Traversable[E]]

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]]
//
//  def delete(id: UUID)(implicit ec: ExecutionContext): Future[Try[UUID]]
}

trait TaskRepository extends Repository[Task] {
  def selectTaskToVerify(taskset: Option[String])(implicit ec: ExecutionContext): Future[Task]
}

trait VerificationRepository extends Repository[Verification] {
  def getLink(verification: Verification)(implicit ec: ExecutionContext): Future[Link]
}

trait SimpleValidatorStatsRepository extends Repository[SimpleValidatorStats] {
  def getStats(verification: Verification)(implicit ec: ExecutionContext): Future[Option[SimpleValidatorStats]]
}

import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

abstract class MongoRepository[E <: MongoEntity : OWrites : Reads] extends Repository[E] {


  /** Mongo collection deserializable to [E] */
  def col(implicit ec: ExecutionContext): Future[JSONCollection]

  def save(entity: E)(implicit ec: ExecutionContext): Future[E] = {
    col flatMap(_.update(selector = Json.obj("_id" -> entity._id), update = entity, upsert = true) map {
      case success: WriteResult if success.ok => entity
      case failure: WriteResult => throw new Exception(failure.message)
    })
  }

  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]] = {
    col flatMap (_.find(Json.obj("_id" -> id.toString)).one[E])
  }

  def findById()(implicit ec: ExecutionContext): Future[Option[E]] = {
    col flatMap (_.find(Json.obj()).one[E])
  }

  def search(name: String, value: String
                      )(implicit ec: ExecutionContext): Future[Traversable[E]] = {
    col flatMap (_.find(Json.obj(name -> value)).cursor[E]().collect[List]())
  }

  def search(name: String, values: Traversable[String])
            (implicit ec: ExecutionContext): Future[Traversable[E]] = {
    col flatMap (_.find(Json.obj(name -> Json.obj("$in" -> values))).cursor[E]().collect[List]())
  }

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]] = {
    col flatMap (_.find(Json.obj()).cursor[E]().collect[List]())
  }
}

import reactivemongo.play.json.collection._
import reactivemongo.play.json.collection.JSONCollection

class TasksetRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[Taskset] {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("tasksets"))
}

class TaskRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi)
extends MongoRepository[Task] with TaskRepository{

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("tasks"))

  override def selectTaskToVerify(taskset: Option[String])(implicit ec: ExecutionContext): Future[Task] = {

    import JSONBatchCommands.AggregationFramework.{Sample, Match, AggregationResult}

    taskset match {
      case Some(ts) =>
        val matchOp = Match(Json.obj("taskset" -> ts))
        col flatMap (_.aggregate(matchOp, List(Sample(1))).map(_.head[Task].head))
      case None =>
        col flatMap (_.aggregate(Sample(1)).map(_.head[Task].head))
     }
  }
}

class LinkRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[Link] {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("links"))
}

class VerificationRepo @Inject()(
                                 val reactiveMongoApi: ReactiveMongoApi,
                                 val linkRepo: LinkRepo,
                                 val taskRepo: TaskRepo
                               )
  extends MongoRepository[Verification]
  with VerificationRepository {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("verifications"))

  override def getLink(verification: Verification)
                      (implicit ec: ExecutionContext): Future[Link] = {
    val linkFuture = for {
      task <- taskRepo.findById(verification.task_id)
      link <- linkRepo.findById(task.get.link_id)
    } yield link
    linkFuture flatMap  {
      case Some(l) => Future.successful(l)
      case _ => Future.failed(new Exception("Database damaged"))
    }
  }
}

class SimpleValidatorStatsRepo @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[SimpleValidatorStats]
    with SimpleValidatorStatsRepository {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("simpleValidatorStats"))

  override def getStats(verification: Verification)
                       (implicit ec: ExecutionContext): Future[Option[SimpleValidatorStats]] = {
    col flatMap (_.find(Json.obj("task_id" -> verification.task_id.toString)).one[SimpleValidatorStats])
  }
}

class UserRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[User] {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("users"))
}
