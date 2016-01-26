package controllers

import javax.inject.{Inject, Singleton}

import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import play.modules.reactivemongo._
import play.modules.reactivemongo.json.collection._

/**
  * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
  * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
  *
  * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
  */
@Singleton
class Tasksets @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Users])

  def collection: JSONCollection = db.collection[JSONCollection]("Tasksets")
}
