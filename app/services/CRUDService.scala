package services

import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

/**
  * Generic async CRUD service trait
  * @param E type of entity
  * @param ID type of identity of entity (primary key)
  */
trait CRUDService[E, ID] {

  def findById(id: ID): Future[Option[E]]

  def findByCriteria(criteria: Map[String, Any], limit: Int): Future[Traversable[E]]

  def create(entity: E): Future[Either[String, ID]]

  def bulkCreate(entities: Stream[E]): Future[Either[String, Boolean]]

  def update(id: ID, entity: E): Future[Either[String, ID]]

  def delete(id: ID): Future[Either[String, ID]]
}

import models.Identity
import play.api.libs.json._
import reactivemongo.api._

/**
  * Abstract {{CRUDService}} impl backed by JSONCollection
  */
abstract class MongoCRUDService[E: Format, ID: Format](implicit identity: Identity[E, ID])
    extends CRUDService[E, ID] {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import play.modules.reactivemongo.json._
  import play.modules.reactivemongo.json.collection.JSONCollection

  /** Mongo collection deserializable to [E] */
  def collection: JSONCollection

  def idField: String = "_id"

  override def findById(id: ID): Future[Option[E]] = collection.
      find(Json.obj(identity.name -> id)).
      one[E]

  override def findByCriteria(criteria: Map[String, Any], limit: Int): Future[Traversable[E]] =
    findByCriteria(CriteriaJSONWriter.writes(criteria), limit)

  private def findByCriteria(criteria: JsObject, limit: Int): Future[Traversable[E]] =
    collection.
        find(criteria).
        cursor[E](readPreference = ReadPreference.primary).
        collect[List](limit)

  override def create(entity: E): Future[Either[String, ID]] = {
    val doc = Json.toJson(entity).as[JsObject]
    collection.insert(doc) map {
      case success: WriteResult if success.ok => Right(identity.of(entity).get)
      case failure: WriteResult => Left(failure.message)
    }
  }

  override def bulkCreate(entities: Stream[E]): Future[Either[String, Boolean]] = {
    val documents = entities.map(entity => Json.toJson(entity).as[JsObject])
    collection.bulkInsert(documents, false) map {
      case success: WriteResult if success.ok => Right(true)
      case failure: WriteResult => Left(failure.errmsg.getOrElse("<none>"))
    }
  }

  override def update(id: ID, entity: E): Future[Either[String, ID]] = {
    val doc = Json.toJson(entity).as[JsObject]
    collection.update(Json.obj(identity.name -> id), doc) map {
      case success: WriteResult if success.ok => Right(id)
      case failure: WriteResult => Left(failure.message)
    }
  }

  override def delete(id: ID): Future[Either[String, ID]] = {
    collection.remove(Json.obj(identity.name -> id)) map {
      case success: WriteResult if success.ok => Right(id)
      case failure: WriteResult => Left(failure.message)
    }
  }
}

object CriteriaJSONWriter extends Writes[Map[String, Any]] {
  val toJsValue: PartialFunction[Any, JsValue] = {
    case v: String => JsString(v)
    case v: Int => JsNumber(v)
    case v: Long => JsNumber(v)
    case v: Double => JsNumber(v)
    case v: Boolean => JsBoolean(v)
    case obj: JsValue => obj
    case map: Map[String, Any]@unchecked => CriteriaJSONWriter.writes(map)
    case coll: Traversable[_] => JsArray(coll.map(toJsValue(_)).toSeq)
    case null => JsNull
    case other => throw new IllegalArgumentException(s"Criteria value type not supported: $other")
  }

  override def writes(criteria: Map[String, Any]): JsObject = JsObject(criteria.mapValues(toJsValue(_)).toSeq)
}
