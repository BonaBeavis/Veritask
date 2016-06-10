package controllers

import java.io.FileReader
import java.net.URL
import java.util.UUID
import javax.inject.{Inject, Singleton}

import config.ConfigBanana
import models.Taskset.tasksetForm
import models.{Task, _}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import services._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class TasksetRequest[A](val taskset: Taskset, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class Tasksets @Inject()(
                          val tasksetRepo: TasksetMongoRepo,
                          val taskRepo: TaskMongoRepo,
                          val linkRepo: LinkMongoRepo,
                          val verificationRepo: VerificationMongoRepo,
                          val validator: SimpleValidator,
                          val messagesApi: MessagesApi)
  extends Controller
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
    Ok(views.html.createTasksetForm(tasksetForm.fillAndValidate(request.taskset)))
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
    (Action andThen TasksetAction(tasksetId)).async(parse.multipartFormData) { request =>
      request.body.file("linkset") match {
        case Some(linkset) => parseLinksetFile(linkset.ref.file) match {
          case Success(links) =>
            val tasks = links.map(link => new Task(UUID.randomUUID(), UUID.fromString(tasksetId), link._id, Map("val" -> "key")))
            for {
              linkS <- Future.sequence(links.map(linkRepo.save))
              taskS <- Future.sequence(tasks.map(taskRepo.save))
            } yield Ok(taskS.size + " Tasks upserted, " + linkS.size + " Links upserted" )
          case Failure(failure) => Future.successful(BadRequest("File could not be parsed"))
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
    Link(uuid, s, p, o)
  }

  def updateTaskAttributes(task: Task): Future[Task] = {

    val taskset = tasksetRepo.findById(task.taskset) flatMap {
      case Some(ts) => Future.successful(ts)
      case None => Future.failed(new Exception("Database corrupt"))
    }

    val link = linkRepo.findById(task.link_id) flatMap {
      case Some(l) => Future.successful(l)
      case None => Future.failed(new Exception("Database corrupt"))
    }

    for {
      taskset <- taskset
      link <- link
      subQueryString = taskset.subjectAttributesQuery.replaceAll(
        "\\{\\{\\s*linkSubject\\s*\\}\\}", "<" + link.linkSubject + ">")
      objQueryString = taskset.objectAttributesQuery.replaceAll(
        "\\{\\{\\s*linkObject\\s*\\}\\}", "<" + link.linkSubject + ">")
      subAttributes <- queryAttribute(new URL(taskset.subjectEndpoint), subQueryString)
      objAttributes <- queryAttribute(new URL(taskset.objectEndpoint), objQueryString)
      updatedTask = task.copy(subjectAttributes = subAttributes, objectAttributes = objAttributes)
      savedTask <- taskRepo.save(updatedTask)
    } yield savedTask
  }

  def queryAttribute(endpoint: URL, queryString: String): Future[Map[String, String]] = {
    import ops._
    import sparqlOps._
    import sparqlHttp.sparqlEngineSyntax._

     val result = for {
      query <- parseSelect(queryString)
      solutions <- endpoint.executeSelect(query, Map())
    } yield {
      val solution = solutions.iterator.next
      solution.vars.map(a => a -> solution.get(a).toString).toMap
    }
    result match {
      case Success(attributes) => Future.successful(attributes)
      case Failure(t) => Future.failed(t)
    }
  }

  def selectTask(): Future[Task] = {
    taskRepo.findById().map(_.get)
  }

  def getTask = Action.async { request =>

    for {
      task <- taskRepo.findById()
      taskset <- tasksetRepo.findById(task.get.taskset)
      newTask <- updateTaskAttributes(task.get)
      json = Json.obj("task" -> Json.toJson(task), "template" -> JsString(taskset.get.template))
    } yield Ok(Json.toJson(json))
  }

  def processVerificationPost() = Action.async(BodyParsers.parse.json) { request =>
    val verification = request.body.validate[Verification]
    verification.fold(
      errors => {
        Future(BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toJson(errors))))
      },
      verification => {
        for {
          verification <- verificationRepo.save(verification)
          validation <- validator.validate(verification)
        } yield Ok(Json.toJson(validation.toString))
      }
    )
  }

  def dumpVerification = Action.async {
    import ops._
    import recordBinder._
    for {
      verification <- verificationRepo.findById()
      task <- taskRepo.findById(verification.get.task_id)
      link <- linkRepo.findById(task.get.link_id)
    } yield Ok(turtleWriter.asString(VerificationDump(
      verification.get._id.toString,
      verification.get.verifier.toString,
      link.get.linkSubject,
      link.get.predicate,
      link.get.linkObject, Some(true)
    ).toPG.graph,"").get)
  }
}