package services

import java.util.UUID

import models.{Link, MongoEntity, Task, Taskset}

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{MultiBulkWriteResult, WriteResult}

import scala.util.Try

/**
  * Generic async CRUD service trait
  */
trait Repository[E] {

  def save(entity: E)(implicit ec: ExecutionContext): Future[Try[E]]

  def save(entities: Stream[E])(implicit ec: ExecutionContext): Future[Try[Int]]

  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]]

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]]
//
//  def delete(id: UUID)(implicit ec: ExecutionContext): Future[Try[UUID]]
}

import reactivemongo.api._

/**
  * Abstract {{CRUDService}} impl backed by JSONCollection
  */
abstract class MongoRepository[E <: MongoEntity : OWrites : Reads] extends Repository[E] {

  import reactivemongo.play.json._
  import reactivemongo.play.json.collection.JSONCollection

  /** Mongo collection deserializable to [E] */
  def collection(implicit ec: ExecutionContext): JSONCollection

  def save(entity: E)(implicit ec: ExecutionContext): Future[Try[E]] = {
    collection.insert(entity) map {
      case success: WriteResult if success.ok => Try(entity)
      case failure: WriteResult => throw new Exception(failure.message)
    }
  }

  def save(entities: Stream[E])(implicit ec: ExecutionContext): Future[Try[Int]] = {
    def e = entities.map(Json.toJson(_).as[JsObject]).toStream
    collection.bulkInsert(e, ordered = true) map {
      case success: MultiBulkWriteResult if success.ok => Try(success.n)
      case failure: MultiBulkWriteResult => throw new Exception(failure.errmsg.get)
    }
  }

  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]] = {
    collection.find(Json.obj("_id" -> id.toString)).one[E]
  }

  def findAll()(implicit ec: ExecutionContext): Future[Traversable[E]] = {
    collection.find(Json.obj()).cursor[E]().collect[List]()
  }
}

import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import reactivemongo.api.DB
import reactivemongo.api._
import scala.concurrent.Future

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class TasksetMongoRepo extends MongoRepository[Taskset] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def collection(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("tasksets")
}

class TaskMongoRepo extends MongoRepository[Task] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def collection(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("tasks")
}

class LinkMongoRepo extends MongoRepository[Link] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def collection(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("links")
}