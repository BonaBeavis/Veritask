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
import play.api.libs.ws._
import services._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class TasksetRequest[A](val taskset: Taskset, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class Tasksets @Inject() (
                           val tasksetRepo: TasksetRepo,
                           val taskRepo: TaskRepo,
                           val linkRepo: LinkRepo,
                           val userRepo: UserRepo,
                           val verificationRepo: VerificationRepo,
                           val validator: SimpleValidator,
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
            val tasks = links.map(link => new Task(
              UUID.randomUUID(),
              UUID.fromString(tasksetId),
              link._id,
              None,
              None))
            for {
              linkS <- Future.sequence(links.map(linkRepo.save))
              taskS <- Future.sequence(tasks.map(taskRepo.save))
            } yield Ok(taskS.size + " Tasks upserted, " + linkS.size + " Links upserted")
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
    Link(uuid, s, p, o, None)
  }

  def updateTaskAttributes(task: Task, taskset: Taskset, link: Link): Future[Task] = {
      val subQueryString = taskset.subjectAttributesQuery.map(_.replaceAll(
        "\\{\\{\\s*linkSubject\\s*\\}\\}", "<" + link.linkSubject + ">"
      ))
      val objQueryString = taskset.objectAttributesQuery.map(_.replaceAll(
        "\\{\\{\\s*linkObject\\s*\\}\\}", "<" + link.linkObject + ">"
      ))

    for {
      subAttributes <- queryAttribute(taskset.subjectEndpoint, subQueryString)
      objAttributes <- queryAttribute(taskset.objectEndpoint, objQueryString)
      updatedTask = task.copy(subjectAttributes = subAttributes, objectAttributes = objAttributes)
      savedTask <- taskRepo.save(updatedTask)
    } yield savedTask
  }

  def queryAttribute(endpointOpt: Option[String], queryStringOpt: Option[String]): Future[Option[Map[String, String]]] = {
    import ops._
    import sparqlOps._
    import sparqlHttp.sparqlEngineSyntax._

    val result = (endpointOpt, queryStringOpt) match {
      case (Some(endpoint), Some(queryString)) =>
        val endpointURL = new URL(endpoint)
        for {
          query <- parseSelect(queryString)
          solutions <- endpointURL.executeSelect(query, Map())
          solution = solutions.iterator.next
        } yield {
          Some(solution.vars.map(a => a -> solution.get(a).toString).toMap)
        }
      case _ => Try(None)
    }

    result match {
      case Success(attributes) => Future.successful(attributes)
      case Failure(t) => Future.failed(t)
    }
  }

  def selectTask(): Future[Task] = {
    for {
      task <- taskRepo.findById()
      if task.nonEmpty
      taskset <- tasksetRepo.findById(task.get.taskset)
      link <- linkRepo.findById(task.get.link_id)
      updatedTask <- updateTaskAttributes(task.get, taskset.get, link.get)
      if taskset.nonEmpty && link.nonEmpty
    } yield updatedTask
  }

  def getUser(name: String): Future[User] = {
    userRepo.search("name", name) flatMap {
      case users: Traversable[User] if users.nonEmpty => Future(users.head)
      case _ =>
        val groups = configuration.getLongSeq("veritask.groups").get
        userRepo.save(User(
          UUID.randomUUID(),
          name,
          scala.util.Random.nextInt(groups.size),
          List(System.currentTimeMillis())
          )
        )
    }
  }

  def getTask(name: String, taskset: Option[String]) = Action.async {
    getUser(name) flatMap {
      case u:User if isTurn(u) =>
        val userStamped = u.copy(timeStamps = System.currentTimeMillis() :: u.timeStamps)
        val body = for {
          user <- userRepo.save(userStamped)
          //task <- taskRepo.selectTaskToVerify
          task <- taskRepo.selectTaskToVerify(taskset)
          taskset <- tasksetRepo.findById(task.taskset)
          link <- linkRepo.findById(task.link_id)
          updatedTask <- updateTaskAttributes(task, taskset.get, link.get)
          json = Json.obj(
            "verifier" -> user._id,
            "link" -> Json.toJson(link),
            "task" -> Json.toJson(updatedTask),
            "template" -> JsString(taskset.get.template)
          )
        } yield Ok(Json.toJson(json))
        body.recover { case t:Throwable => Ok(Json.toJson(JsNull))}
      case _ => Future(Ok(Json.toJson(JsNull)))
    }
  }


  def isTurn(user: User): Boolean = {
    val timeSinceLastRequest = System.currentTimeMillis() - user.timeStamps.head
    val groups = configuration.getLongSeq("veritask.groups").get
    timeSinceLastRequest > groups(user.group) || user.name == "testUser"
  }


  def processVerificationPost() = Action.async(BodyParsers.parse.json) { request =>
    val verification = request.body.validate[Verification]
    verification.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      verification => {
        val estimation = for {
          veri <- verificationRepo.save(verification)
          est <- validator.process(veri)
          throwaway <- dumpVerification(veri, est)
        } yield est

        estimation map {
          case a: Double if a > 0.5 => Ok(Json.toJson(true))
          case _ => Ok(Json.toJson(false))
        }
      }

    )
  }

  def dumpVerification(verification: Verification, estimation: Double) =  {
    val url = configuration.getString("triplestore.uri").get
    val request: WSRequest = ws.url(url)
    import ops._
    val test = for {
      link <- verificationRepo.getLink(verification)
    } yield turtleWriter.asString(
      VerificationDump(
        verification._id,
        verification.verifier.toString,
        link.copy(estimation = Some(estimation)),
        Some(true)
      ).toPG.graph, "").get
    for {
      test <- test
      req <- request.withHeaders("Content-Type" -> "text/turtle").withMethod("POST").post(test)
    } yield "Done"
  }
}

