package models

/**
  * Created by beavis on 06.06.16.
  */
sealed trait Validation {
  case object Yes extends Validation
  case object No extends Validation
}
