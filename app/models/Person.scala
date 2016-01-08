package models

import config.ConfigBanana
import play.api.libs.json.Json

/**
  * Created by beavis on 27.11.15.
  */
case class Person(name: String, nickname: Option[String] = None) {
}

object Person extends ConfigBanana {
  import ops._
  import recordBinder._

  val clazz = URI("http://example.com/Person#class")
  implicit val classUris = classUrisFor[Person](clazz)

  val name = property[String](foaf.name)
  val nickname = optional[String](foaf("nickname"))

  implicit val container = URI("http://example.com/persons/")
  implicit val binder = pgb[Person](name, nickname)(Person.apply, Person.unapply)
  implicit val personFormat = Json.format[Person]
}

