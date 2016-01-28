package services

import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

/**
  * Generic async CRUD service trait
  *
  * @param E type of entity
  * @param ID type of identity of entity (primary key)
  */
trait CRUDService[E, ID] {

  def findById(id: ID): Future[Option[E]]

  def findByCriteria(criteria: Map[String, Any], limit: Int): Future[Traversable[E]]

  def create(entity: E): Future[E]

  def update(id: ID, entity: E): Future[ID]

  def delete(id: ID): Future[ID]
}

import play.api.libs.json._

/**
  * Abstract {{CRUDService}} impl backed by JSONCollection
  */
abstract class MongoCRUDService[E: Format, ID: Format]
    extends CRUDService[E, ID] {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import play.modules.reactivemongo.json._
  import play.modules.reactivemongo.json.collection.JSONCollection

  /** Mongo collection deserializable to [E] */
  def collection: JSONCollection

  override def findById(id: ID): Future[Option[E]] = collection.
    find(Json.obj("_id" -> id)).
      one[E]

  //  override def findByCriteria(criteria: Map[String, Any], limit: Int): Future[Traversable[E]] =
  //    findByCriteria(CriteriaJSONWriter.writes(criteria), limit)
  //
  //  private def findByCriteria(criteria: JsObject, limit: Int): Future[Traversable[E]] =
  //    collection.
  //        find(criteria).
  //        cursor[E](readPreference = ReadPreference.primary).
  //        collect[List](limit)

  override def create(entity: E): Future[E] = {
    collection.insert(Json.toJson(entity).as[JsObject]).flatMap {
      case success: WriteResult if success.ok => Future.successful(entity)
      case failure: WriteResult => Future.failed(failure)
    }
  }

  //  override def bulkCreate(entities: Stream[E]): Future[Either[String, Boolean]] = {
  //    val documents = entities.map(entity => Json.toJson(entity).as[JsObject])
  //    collection.bulkInsert(documents, false) map {
  //      case success: WriteResult if success.ok => Right(true)
  //      case failure: WriteResult => Left(failure.errmsg.getOrElse("<none>"))
  //    }
  //  }

  override def update(id: ID, entity: E): Future[ID] = {
    val doc = Json.toJson(entity).as[JsObject]
    collection.update(Json.obj("_id" -> id), doc) flatMap {
      case success: WriteResult if success.ok => Future.successful(id)
      case failure: WriteResult => Future.failed(failure)
    }
  }

  override def delete(id: ID): Future[ID] = {
    collection.remove(Json.obj("_id" -> id)) flatMap {
      case success: WriteResult if success.ok => Future.successful(id)
      case failure: WriteResult => Future.failed(failure)
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
