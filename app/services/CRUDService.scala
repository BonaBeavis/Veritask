package services

import java.util.UUID

import models.{MongoEntity, Taskset}

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult

import scala.util.Try

/**
  * Generic async CRUD service trait
  */
trait CRUDService[E] {

  def save(entity: E)(implicit ec: ExecutionContext): Future[Try[E]]

//  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]]
//
//  def findAll(id: UUID)(implicit ec: ExecutionContext): Future[Traversable[E]]
//
//  def delete(id: UUID)(implicit ec: ExecutionContext): Future[Try[UUID]]
}

import reactivemongo.api._

/**
  * Abstract {{CRUDService}} impl backed by JSONCollection
  */
abstract class MongoCRUDService[E <: MongoEntity : OWrites] extends CRUDService[E] {

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

//  def findById(id: UUID)(implicit ec: ExecutionContext): Future[Option[E]] = {
//    collection.find(Json.obj("id" -> id.toString)).one[E]
//  }
//
//
//  def findAll(id: UUID, entity: E)(implicit ec: ExecutionContext): Future[Traversable[E]] = {
//    collection.find(Json.obj("id" -> id.toString)).cursor[E].collect[List]()
//
//  }
}

import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import reactivemongo.api.DB
import reactivemongo.api._
import scala.concurrent.Future

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class TasksetMongoService extends MongoCRUDService[Taskset] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  override def collection(implicit ec: ExecutionContext): JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("vessels")

}