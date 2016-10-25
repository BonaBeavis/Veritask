package controllers
import javax.inject.Inject

import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services._

/** Renders a view with all the links a user has verified.
  *
  * TODO: finish the stump
  */
class Users @Inject() (
                        val userRepo: UserMongoRepo,
                        val messagesApi: MessagesApi
                      )
  extends Controller with I18nSupport {

  def stats(name: String) = Action.async {
    userRepo.search("name", name) map {
      case users: Traversable[User] if users.nonEmpty =>
        Ok(Json.toJson(users.head))
      case _ => NoContent
    }
  }
}
