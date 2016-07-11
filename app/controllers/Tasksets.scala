package controllers

import java.io.FileReader
import java.util.UUID
import javax.inject.{Inject, Singleton}

import config.ConfigBanana
import models.Taskset.tasksetForm
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.mvc._
import services._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class TasksetRequest[A](
  val taskset: Taskset, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class Tasksets @Inject() (
                           val tasksetRepo: TasksetMongoRepo,
                           val taskRepo: TaskMongoRepo,
                           val linkRepo: LinkMongoRepo,
                           val messagesApi: MessagesApi,
                           val ws: WSClient,
                           val configuration: play.api.Configuration
) extends Controller
  with I18nSupport
  with ConfigBanana {

  def TasksetAction(id: String) = new ActionRefiner[Request, TasksetRequest] {
    def refine[A](input: Request[A]) =
      tasksetRepo.findById(UUID.fromString(id)) map {
        case Some(taskset) => Right(new TasksetRequest(taskset, input))
        case None => Left(NotFound)
      }
  }

  def listTasksetsView = Action.async {
    tasksetRepo.findAll() map {
      case tasksets: List[Taskset] => Ok(views.html.tasksets(tasksets))
      case _ => InternalServerError("Could not get Tasksets")
    }
  }

  def tasksetView = Action {
    Ok(views.html.createTasksetForm(tasksetForm))
  }

  def updateTasksetView(id: String) =
    (Action andThen TasksetAction(id)) { request =>
      Ok(views.html.createTasksetForm(
        tasksetForm.fillAndValidate(request.taskset)))
    }

  def saveTaskset(id: String) = Action.async { implicit request =>
    val newId = if (id.isEmpty) UUID.randomUUID() else UUID.fromString(id)
    tasksetForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest("Form validation failed")),
      taskset => tasksetRepo.save(taskset.copy(_id = newId)) map {
        case created: Taskset => Redirect(routes.Tasksets.listTasksetsView())
      }
    )
  }

  def uploadLinksetFile(tasksetId: String) =
    (Action andThen TasksetAction(tasksetId)).async(parse.multipartFormData) {
      request =>
      request.body.file("linkset") match {
        case Some(linkset) => parseLinksetFile(linkset.ref.file) match {
          case Success(links) =>
            val tasks = links.map(link => new Task(
              UUID.randomUUID(),
              UUID.fromString(tasksetId),
              link._id,
              None,
              None))
            for {
              linkS <- Future.sequence(links.map(linkRepo.save))
              taskS <- Future.sequence(tasks.map(taskRepo.save))
            } yield Ok(
              taskS.size + " Tasks upserted, " + linkS.size + " Links upserted")
          case Failure(failure) => Future.successful(
            BadRequest("File could not be parsed"))
        }
        case None => Future.successful(BadRequest("No RDF file"))
      }
    }

  def parseLinksetFile(linksetFile: java.io.File): Try[Iterable[Link]] = {
    import ops._
    turtleReader.read(new FileReader(linksetFile), "") map {
      graph => graph.triples map (linkFromTriple(_))
    }
  }

  def linkFromTriple(triple: Rdf#Triple): Link = {
    import ops._
    val (subject, predicate, objectt) = fromTriple(triple)
    val s = subject.toString()
    val p = predicate.toString()
    val o = objectt.toString()
    val uuid = UUID.nameUUIDFromBytes((s + p + o).getBytes)
    Link(uuid, s, p, o, None)
  }
}

