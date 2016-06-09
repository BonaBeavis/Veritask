package models

/**
  * Created by beavis on 06.06.16.
  */
sealed trait Validation {
  case object right extends Validation
  case object wrong extends Validation
  case object unsure extends Validation
}
