package models

/**
  * Type class providing identity manipulation methods
  */
trait Identity[E, Id] {

  def name: String

  def of(entity: E): Option[Id]

  def set(entity: E, id: Id): E

  def generateID(entity: E): Id
}

