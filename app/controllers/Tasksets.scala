package controllers

import java.io.FileReader
import java.util.UUID
import javax.inject.{Inject, Singleton}

import config.ConfigBanana
import models.Taskset.tasksetForm
import models._
import org.apache.commons.codec.digest.DigestUtils
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection._
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import services.{LinkMongoRepo, TaskMongoRepo, TasksetMongoRepo}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class TasksetRequest[A](val taskset: Taskset, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class Tasksets @Inject()(
                          val tasksetRepo: TasksetMongoRepo,
                          val taskRepo: TaskMongoRepo,
                          val linkRepo: LinkMongoRepo,
                          val messagesApi: MessagesApi)
  extends Controller
    with I18nSupport
    with ConfigBanana {


  def uploadLinksetFile(tasksetId: String) =
    (Action andThen TasksetAction(tasksetId)).async(parse.multipartFormData) { request =>
      request.body.file("linkset") match {
        case Some(linkset) => parseLinksetFile(linkset.ref.file) match {
          case Success(links) => addLinks(links, request.taskset._id.toString) map {
            case Success(numTasksAdded) => Created("Created " + numTasksAdded + " Tasks")
            case Failure(failure) => BadRequest(failure.getMessage)
          }
          case Failure(failure) => Future.successful(BadRequest("File could not be parsed"))
        }
        case None => Future.successful(BadRequest("No RDF file"))
      }
    }

  def addLinks(links: Iterable[Link], tasksetId: String): Future[Try[Int]] = {
    val tasks = links map(link => new Task(UUID.randomUUID(), UUID.fromString(tasksetId), link._id))
    for {
      numberLinks <-linkRepo.save(links.toStream)
      numberTasks <-taskRepo.save(tasks.toStream)
    } yield numberTasks
  }

  def parseLinksetFile(linksetFile: java.io.File): Try[Iterable[Link]] = {
    import ops._
    jsonldReader.read(new FileReader(linksetFile), "") map {
      graph => graph.triples map (linkFromTriple(_))
    }
  }

  def linkFromTriple(triple: Rdf#Triple): Link = {
    import ops._
    val (subject, predicate, objectt) = fromTriple(triple)
    val s = subject.stringValue()
    val p = predicate.stringValue()
    val o = objectt.stringValue()
    val uuid = UUID.nameUUIDFromBytes((s + p + o).getBytes)
    Link(uuid, s, p, o)
  }

  def TasksetAction(id: String) = new ActionRefiner[Request, TasksetRequest] {
    def refine[A](input: Request[A]) =
      tasksetRepo.findById(UUID.fromString(id)) map {
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
      taskset => tasksetRepo.save(taskset) map {
        case Success(taskset) => Created("yo")
        case Failure(ex) => Ok("crap")
      }
    )
  }

  def createTaskset = Action {
    Ok(views.html.taskset(tasksetForm))
  }

  def viewTaskset(id: String) = Action.async {
    tasksetRepo.findById(UUID.fromString(id)) map {
      case Some(taskset) => Ok(views.html.taskset(tasksetForm.fillAndValidate(taskset)))
      case None => NoContent
    }
  }

  def listTasksets = Action.async {
    tasksetRepo.findAll() map {
      case tasksets: List[Taskset] => Ok(views.html.tasksets(tasksets))
      case _ => InternalServerError("Could not get Tasksets")
    }
  }

//  def geTasksetNames: Future[List[String]] = {
//    val cursor: Cursor[Taskset] = collection.
//      find(Json.obj("active" -> true)).
//      cursor[Taskset]()
//    cursor.collect[List]() map {
//      _ map {
//        _.name
//      }
//    }
//  }

  //def collection: JSONCollection = db.collection[JSONCollection]("Tasksets")
}