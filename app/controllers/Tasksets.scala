package controllers

import javax.inject.{Inject, Singleton}

import models.Taskset.tasksetForm
import models.{Taskset, TasksetDao}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection._
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

@Singleton
class Tasksets @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  def createTaskset = Action.async { implicit request =>
    tasksetForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest("Form validation failed")),
      taskset => TasksetDao.create(taskset) map {
        case success: WriteResult if success.ok => Created(views.html.index())
        case failure: WriteResult => InternalServerError(failure.message)
      }
    )
  }

  def viewTaskset = Action {
    Ok(views.html.taskset(tasksetForm))
  }

  def geTasksetNames: Future[List[String]] = {
    val cursor: Cursor[Taskset] = collection.
      find(Json.obj("active" -> true)).
      cursor[Taskset]()
    cursor.collect[List]() map {
      _ map {
        _.name
      }
    }
  }

  def collection: JSONCollection = db.collection[JSONCollection]("Tasksets")
}
