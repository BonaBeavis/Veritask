package controllers

import java.util.UUID
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

  def upsertTaskset = Action.async { implicit request =>
    tasksetForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest("Form validation failed")),
      taskset => TasksetDao.save(taskset) map {
        case success: WriteResult if success.ok => Created(views.html.index())
        case failure: WriteResult => InternalServerError(failure.message)
      }
    )
  }

  def viewTaskset(id: Option[String]) = Action.async {
    id match {
      case Some(_id) => TasksetDao.findById(UUID.fromString(_id)) map {
        case Some(taskset) =>
          Ok(views.html.taskset(tasksetForm.fillAndValidate(taskset)))
        case None => NoContent
      }
      case None => Future.successful(Ok(views.html.taskset(tasksetForm)))
    }
  }

  def listTasksets = Action.async {
    TasksetDao.findAll() map {
      case tasksets: List[Taskset] => Ok(views.html.listTasksets(tasksets))
      case _ => InternalServerError("Could not get Tasksets")
    }
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
