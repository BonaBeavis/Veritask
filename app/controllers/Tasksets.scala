package controllers

import java.io.FileReader
import java.util.UUID
import javax.inject.{Inject, Singleton}

import config.ConfigBanana
import models.Taskset.tasksetForm
import models.{Task, TaskDao, Taskset, TasksetDao}
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
import scala.util.{Failure, Success, Try}

class TasksetRequest[A](val taskset: Taskset, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class Tasksets @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller
    with MongoController
    with ReactiveMongoComponents
    with I18nSupport
    with ConfigBanana {


  def uploadLinksetFile(tasksetId: String) =
    (Action andThen TasksetAction(tasksetId)).async(parse.multipartFormData) { request =>
      request.body.file("linkset") match {
        case Some(linkset) => parseLinksetFile(linkset.ref.file, request.taskset) match {
          case Success(tasks) => TaskDao.bulkInsert(tasks) map { x => Ok(x.toString + " inserted") }
          case Failure(failure) => Future.successful(BadRequest("File could not be parsed"))
      }
        case None => Future.successful(BadRequest("No RDF file"))
    }
    }

  def parseLinksetFile(linksetFile: java.io.File, taskset: Taskset): Try[Iterable[Task]] = {
    import ops._
    jsonldReader.read(new FileReader(linksetFile), "") map {
      graph => graph.triples map (taskFromTriple(_, taskset))
    }
  }

  def taskFromTriple(triple: Rdf#Triple, taskset: Taskset): Task = {
    import ops._
    val (s, _, o) = fromTriple(triple)
    Task(
      UUID.randomUUID(),
      taskset._id,
      s.toString,
      o.toString
    )
  }

  def TasksetAction(tasksetId: String) = new ActionRefiner[Request, TasksetRequest] {
    def refine[A](input: Request[A]) =
      TasksetDao.findByIdString(tasksetId) map {
        case Some(taskset) => Right(new TasksetRequest(taskset, input))
        case None => Left(NotFound)
      }
  }

  //  def parseLinksetGraph(graph: Rdf#Graph, taskset: Taskset): Iterable[Task] = {
  //    import ops._
  //    graph.triples.map {
  //      x => taskFromTriple(x, taskset)
  //    }
  //  }

  //  def isTripleInTaskset(triple: List[Rdf#URI], taskset: Taskset): Boolean = {
  //    import ops._
  //    import recordBinder._
  //    foldNode[String](triple.getSubject)(_)
  //    triple.getPredicate.getString == taskset.linkPredicate &&
  //    triple.getObject.toUri.fragmentLess.getString == taskset.objectsTarget
  //  }

  def postTaskset = Action.async { implicit request =>
    tasksetForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest("Form validation failed")),
      taskset => TasksetDao.save(taskset) map {
        case success: WriteResult if success.ok => Created(views.html.index())
        case failure: WriteResult => InternalServerError(failure.message)
      }
    )
  }

  def createTaskset = Action {
    Ok(views.html.taskset(tasksetForm))
  }

  def viewTaskset(id: String) = Action.async {
    TasksetDao.findById(UUID.fromString(id)) map {
      case Some(taskset) =>
        Ok(views.html.taskset(tasksetForm.fillAndValidate(taskset)))
      case None => NoContent
    }
  }

  def listTasksets = Action.async {
    TasksetDao.findAll() map {
      case tasksets: List[Taskset] => Ok(views.html.tasksets(tasksets))
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
