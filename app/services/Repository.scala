package services

import java.util.UUID

import com.google.inject.Inject
import models._

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{MultiBulkWriteResult, WriteResult}

import scala.util.Try

trait Repository[E] {

  def save(entity: E)(implicit ec: ExecutionContext): Future[E]

  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]]

  def findById()(implicit ec: ExecutionContext): Future[Option[E]]

  def search(key: String, value: String)(implicit ec: ExecutionContext): Future[Traversable[E]]

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]]
//
//  def delete(id: UUID)(implicit ec: ExecutionContext): Future[Try[UUID]]
}

trait TaskRepository extends Repository[Task] {
  def selectTaskToVerify(implicit ec: ExecutionContext): Future[Task]
}

trait VerificationRepository extends Repository[Verification] {
  def getLink(verification: Verification)(implicit ec: ExecutionContext): Future[Link]
}

abstract class MongoRepository[E <: MongoEntity : OWrites : Reads] extends Repository[E] {

  import reactivemongo.play.json._
  import reactivemongo.play.json.collection.JSONCollection

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

  def search( name: String, value: String
                      )(implicit ec: ExecutionContext): Future[Traversable[E]] = {
    col flatMap (_.find(Json.obj(name -> value)).cursor[E]().collect[List]())
  }

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]] = {
    col flatMap (_.find(Json.obj()).cursor[E]().collect[List]())
  }
}

import reactivemongo.play.json.collection._
import play.api.Play.current
import reactivemongo.play.json._
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

  override def selectTaskToVerify(implicit ec: ExecutionContext): Future[Task] = {

    import JSONBatchCommands.AggregationFramework.{Sample, AggregationResult}

     col flatMap (_.aggregate(Sample(1)).map(_.head[Task].head))
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
                               )  extends MongoRepository[Verification] with VerificationRepository {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("verifications"))

  override def getLink(verification: Verification)(implicit ec: ExecutionContext): Future[Link] = {
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

class SimpleValidatorStatsRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[SimpleValidatorStats] {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("simpleValidatorStats"))
}

class UserRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[User] {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("users"))
}
