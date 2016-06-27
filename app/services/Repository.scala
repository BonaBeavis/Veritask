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
  def col(implicit ec: ExecutionContext): JSONCollection

  def save(entity: E)(implicit ec: ExecutionContext): Future[E] = {
    col.update(selector = Json.obj("_id" -> entity._id), update = entity, upsert = true) map {
      case success: WriteResult if success.ok => entity
      case failure: WriteResult => throw new Exception(failure.message)
    }
  }

  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]] = {
    col.find(Json.obj("_id" -> id.toString)).one[E]
  }

  def findById()(implicit ec: ExecutionContext): Future[Option[E]] = {
    col.find(Json.obj()).one[E]
  }

  def search( name: String, value: String
                      )(implicit ec: ExecutionContext): Future[Traversable[E]] = {
    col.find(Json.obj(name -> value)).cursor[E]().collect[List]()
  }

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]] = {
    col.find(Json.obj()).cursor[E]().collect[List]()
  }
}

import reactivemongo.play.json.collection._
import play.api.Play.current
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

class TasksetRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends MongoRepository[Taskset] {
  override def col(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("tasksets")
}

class TaskRepo extends MongoRepository[Task] with TaskRepository{
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def col(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("tasks")



  override def selectTaskToVerify(implicit ec: ExecutionContext): Future[Task] = {

    import JSONBatchCommands.AggregationFramework.{Sample, AggregationResult}
    val res: Future[AggregationResult] = col.aggregate(Sample(1))

    res.map(_.head[Task].head)
  }
}

class LinkRepo extends MongoRepository[Link] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def col(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("links")
}

class VerificationRepo @Inject()(
                                 val linkRepo: LinkRepo,
                                 val taskRepo: TaskRepo
                               )  extends MongoRepository[Verification] with VerificationRepository {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def col(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("verifications")

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

class SimpleValidatorStatsRepo extends MongoRepository[SimpleValidatorStats] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def col(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("simpleValidatorStats")
}

class UserRepo extends MongoRepository[User] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def col(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("users")
}
