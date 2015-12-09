package services

/**
  * Created by beavis on 03.12.15.
  */
trait CRUD[T, ID] {
  def create(t: T): T

  def update(T: T)

  def tryFindById(id: ID): Option[T]

  def delete(id: ID)
}
