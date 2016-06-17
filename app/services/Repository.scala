package services

import java.util.UUID

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

abstract class MongoRepository[E <: MongoEntity : OWrites : Reads] extends Repository[E] {

  import reactivemongo.play.json._
  import reactivemongo.play.json.collection.JSONCollection

  /** Mongo collection deserializable to [E] */
  def collection(implicit ec: ExecutionContext): JSONCollection

  def save(entity: E)(implicit ec: ExecutionContext): Future[E] = {
    collection.update(selector = Json.obj("_id" -> entity._id), update = entity, upsert = true) map {
      case success: WriteResult if success.ok => entity
      case failure: WriteResult => throw new Exception(failure.message)
    }
  }

  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]] = {
    collection.find(Json.obj("_id" -> id.toString)).one[E]
  }

  def findById()(implicit ec: ExecutionContext): Future[Option[E]] = {
    collection.find(Json.obj()).one[E]
  }

  def search( name: String, value: String
                      )(implicit ec: ExecutionContext): Future[Traversable[E]] = {
    collection.find(Json.obj(name -> value)).cursor[E]().collect[List]()
  }

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]] = {
    collection.find(Json.obj()).cursor[E]().collect[List]()
  }
}

import reactivemongo.play.json.collection._
import play.api.Play.current

class TasksetRepo extends MongoRepository[Taskset] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def collection(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("tasksets")
}

class TaskRepo extends MongoRepository[Task] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def collection(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("tasks")
}

class LinkRepo extends MongoRepository[Link] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def collection(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("links")
}

class VerificationRepo extends MongoRepository[Verification] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def collection(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("verifications")
}

class SimpleValidatorStatsRepo extends MongoRepository[SimpleValidatorStats] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def collection(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("simpleValidatorStats")
}

class UserRepo extends MongoRepository[User] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def collection(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("users")
}
