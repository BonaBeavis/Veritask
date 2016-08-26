package services

import java.util.UUID

import com.google.inject.Inject
import models.{SimpleValidatorStats, User, _}
import play.api.libs.json.{Json, OWrites, Reads}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json._
import reactivemongo.play.json.collection.{JSONBatchCommands, JSONCollection}

import scala.concurrent.{ExecutionContext, Future}

abstract class MongoRepository[E <: MongoEntity : OWrites : Reads]
  extends Repository[E] {

  def col(implicit ec: ExecutionContext): Future[JSONCollection]

  def save(entity: E)(implicit ec: ExecutionContext): Future[E] = {
    col flatMap(_.update(
      selector = Json.obj("_id" -> entity._id),
      update = entity,
      upsert = true) map {
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

  def search(name: String, value: String)
    (implicit ec: ExecutionContext): Future[Traversable[E]] = {
    col flatMap (_.find(Json.obj(name -> value)).cursor[E]().collect[List]())
  }

  def search(name: String, values: Traversable[String])
            (implicit ec: ExecutionContext): Future[Traversable[E]] = {
    col flatMap (_
      .find(Json.obj(name -> Json.obj("$in" -> values)))
      .cursor[E]()
      .collect[List]())
  }

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]] = {
    col flatMap (_.find(Json.obj()).cursor[E]().collect[List]())
  }

  def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[UUID]] = {
    col flatMap (_.remove(Json.obj("_id" -> uuid))) map {
      case wR: WriteResult if wR.ok => Some(uuid)
      case wR: WriteResult if wR.ok => None
    }
  }
}

class TasksetMongoRepo @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[Taskset] {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("tasksets"))
}

class TaskMongoRepo @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[Task] with TaskRepository{

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("tasks"))

  override def selectTaskToVerify(taskset: Option[String],
                                  verifiedTasks: List[UUID])
    (implicit ec: ExecutionContext): Future[Option[Task]] = {

    import JSONBatchCommands.AggregationFramework.{Match, Sample}

    val queryTasks = Json.obj("_id" -> Json.obj("$nin" -> verifiedTasks))

    taskset match {
      case Some(ts) =>
        val queryTaskset = Json.obj("taskset" -> ts)
        val mmatch = Json.obj("$and" -> Json.arr(queryTasks, queryTaskset))
        col flatMap (
          _.aggregate(Match(mmatch), List(Sample(1)))
          .map(_.head[Task].headOption))
      case None =>
        col flatMap (
          _.aggregate(Match(queryTasks), List(Sample(1)))
          .map(_.head[Task].headOption))
    }
  }
}

class LinkMongoRepo @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[Link] {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("links"))
}

class VerificationMongoRepo @Inject()(
                                  val reactiveMongoApi: ReactiveMongoApi,
                                  val linkRepo: LinkMongoRepo,
                                  val taskRepo: TaskMongoRepo
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

class SimpleValidatorStatsMongoRepo @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[SimpleValidatorStats]
    with SimpleValidatorStatsRepository {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(
      _.collection[JSONCollection]("simpleValidatorStats"))

  override def getStats(verification: Verification)
    (implicit ec: ExecutionContext): Future[Option[SimpleValidatorStats]] = {
    col flatMap (_
      .find(Json.obj("task_id" -> verification.task_id.toString))
      .one[SimpleValidatorStats])
  }
}

class UserMongoRepo @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[User] {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("users"))
}

class EvalDataRepo @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends MongoRepository[EvalData] {

  override def col(implicit ec: ExecutionContext): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("evaldata"))
}