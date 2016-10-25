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

// Request with added taskset request
class TasksetRequest[A](val taskset: Taskset, request: Request[A])
  extends WrappedRequest[A](request)

/** Provides a frontend for managing tasksets and creates tasksets from a
  * definition and a linkset.
  */
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

  /** Adds taskset instance to request.
    *
    * @param uuid UUID of the taskset to add.
    */
  def TasksetAction(uuid: String) = new ActionRefiner[Request, TasksetRequest] {
    def refine[A](input: Request[A]) =
      tasksetRepo.findById(UUID.fromString(uuid)) map {
        case Some(taskset) => Right(new TasksetRequest(taskset, input))
        case None => Left(NotFound)
      }
  }

  /** Renders a list of all tasksets.
    */
  def listTasksetsView = Action.async {
    tasksetRepo.findAll() map {
      case tasksets: List[Taskset] => Ok(views.html.tasksets(tasksets))
      case _ => InternalServerError("Could not get Tasksets")
    }
  }

  /** Renders a taskset creation form
    */
  def tasksetView = Action {
    Ok(views.html.createTasksetForm(tasksetForm))
  }

  /** Renders a form to update a existing taskset.
    *
    * @param uuid UUID of the taskset to add.
    */
  def updateTasksetView(uuid: String) =
    (Action andThen TasksetAction(uuid)) { request =>
      Ok(views.html.createTasksetForm(
        tasksetForm.fillAndValidate(request.taskset)))
    }

  /** Validates and saves a taskset
    *
    * @param uuid UUID of the taskset to add.
    */
  def saveTaskset(uuid: String) = Action.async { implicit request =>
    val newId = if (uuid.isEmpty) UUID.randomUUID() else UUID.fromString(uuid)
    tasksetForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest("Form validation failed")),
      taskset => tasksetRepo.save(taskset.copy(_id = newId)) map {
        case created: Taskset => Redirect(routes.Tasksets.listTasksetsView())
      }
    )
  }

  /** Creates tasks and links from a linkset file.
    *
    * @param tasksetUUID UUID of the taskset to add.
    */
  def uploadLinksetFile(tasksetUUID: String) =
    (Action andThen TasksetAction(tasksetUUID)).async(parse.multipartFormData) {
      request =>
      request.body.file("linkset") match {
        case Some(linkset) => parseLinksetFile(linkset.ref.file) match {
          case Success(links) =>
            val tasks = links.map(link => new Task(
              UUID.randomUUID(),
              UUID.fromString(tasksetUUID),
              link._id,
              None,
              None))
            for {
              numLinksSaved <- linkRepo.bulkSave(links.toSeq)
              numTasksSaved <- taskRepo.bulkSave(tasks.toSeq)
            } yield Ok(
              numLinksSaved + " Tasks upserted, " + numTasksSaved + " Links upserted")
          case Failure(failure) => Future.successful(
            BadRequest("File could not be parsed"))
        }
        case None => Future.successful(BadRequest("No RDF file"))
      }
    }

  /** Creates a list of links from a turtle linkset file.
    *
    * @param linksetFile a turtle formatted file with links
    *
    * @return list of links if the file parses
    */
  def parseLinksetFile(linksetFile: java.io.File): Try[Iterable[Link]] = {
    import ops._
    turtleReader.read(new FileReader(linksetFile), "") map {
      graph => graph.triples map (linkFromTriple(_))
    }
  }

  /** Creates a link from a rdf triple.
    *
    * @param triple a rdf triple
    *
    * @return a Link
    */
  def linkFromTriple(triple: Rdf#Triple): Link = {
    import ops._
    val (subject, predicate, objectt) = fromTriple(triple)
    val s = subject.toString()
    val p = predicate.toString()
    val o = objectt.toString()
    val uuid = UUID.randomUUID()
    Link(uuid, s, p, o, None)
  }
}

