package controllers

import javax.inject.{Inject, Singleton}

import models._
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.modules.reactivemongo._
import play.modules.reactivemongo.json.collection._

import scala.concurrent.Future

/**
  * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
  * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
  * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
  */
@Singleton
class Persons @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Persons])

  def createPerson = Action.async(parse.json) {
    request =>
      /*
     * request.body is a JsValue.
     * There is an implicit Writes that turns this JsValue as a JsObject,
     * so you can call insert() with this JsValue.
     * (insert() takes a JsObject as parameter, or anything that can be
     * turned into a JsObject using a Writes.)
     */
      request.body.validate[Person].map {
        person =>
          // `user` is an instance of the case class `models.User`
          collection.insert(person).map {
            lastError =>
              logger.debug(s"Successfully inserted with LastError: $lastError")
              Created(s"User Created")
          }
      }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def collection: JSONCollection = db.collection[JSONCollection]("persons")
}
