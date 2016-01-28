package models

/**
  * Type class providing identity manipulation methods
  */
trait MongoIdentity[Id] {
  val _id: Option[Id]
}

