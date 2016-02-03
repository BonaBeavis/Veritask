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
import scala.util.{Failure, Success}

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
        case Some(linkset) =>
          jsonldReader.read(new FileReader(linkset.ref.file), "") match {
            case Success(graph) =>
              TaskDao.bulkInsert(parseLinksetGraph(graph, request.taskset)).map { i => Ok(i) }
            case Failure(failure) => Future.successful(InternalServerError(failure))
          }
        case None => Future.successful(BadRequest("No RDF file"))
      }
    }

  def TasksetAction(tasksetId: String) = new ActionRefiner[Request, TasksetRequest] {
    def refine[A](input: Request[A]) =
      TasksetDao.findByIdString(tasksetId) map {
        case Some(taskset) => Right(new TasksetRequest(taskset, input))
        case None => Left(NotFound)
      }
  }

  def parseLinksetGraph(graph: Rdf#Graph, taskset: Taskset): Iterable[Task] = {
    import ops._
    graph.triples.collect {
      case triple: Rdf#Triple if isTripleInTaskset(triple, taskset) =>
        val (s, p, _) = fromTriple(triple)
        Task(UUID.randomUUID(), taskset._id.toString, s.toString, p.toString, s.toString)
    }
  }

  def isTripleInTaskset(triple: Rdf#Triple, taskset: Taskset): Boolean = {
    import ops._
    triple.getSubject.toUri.fragmentLess.getString == taskset.subjectsTarget &&
      triple.getPredicate.getString == taskset.linkPredicate &&
      triple.getObject.toUri.fragmentLess.getString == taskset.objectsTarget
  }

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
