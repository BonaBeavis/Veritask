package controllers

import services._
import config.ConfigBanana
import models.{Link, Task, Taskset, User}

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsNull, JsString, Json}
import play.api.mvc.{Action, Controller}

import java.net.URL
import java.util.UUID
import javax.inject.Inject

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Tasks @Inject() (
  val tasksetRepo: TasksetRepo,
  val taskRepo: TaskRepo,
  val linkRepo: LinkRepo,
  val userRepo: UserRepo,
  val messagesApi: MessagesApi,
  val configuration: play.api.Configuration)
  extends Controller
  with I18nSupport
  with ConfigBanana {

  def getTask(name: String, taskset: Option[String]) = Action.async {
    getUser(name) flatMap {
      case u:User if isTurn(u) =>
        val userStamped = u.copy(
          timeStamps = System.currentTimeMillis() :: u.timeStamps)
        val body = for {
          user <- userRepo.save(userStamped)
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

  def getUser(name: String): Future[User] = {
    userRepo.search("name", name) flatMap {
      case users: Traversable[User] if users.nonEmpty => Future(users.head)
      case _ =>
        val groups = configuration.getLongSeq("veritask.groups").get
        userRepo.save(
          User(
            UUID.randomUUID(),
            name,
            scala.util.Random.nextInt(groups.size),
            List(System.currentTimeMillis())))
    }
  }

  def isTurn(user: User): Boolean = {
    val timeSinceLastRequest = System.currentTimeMillis() - user.timeStamps.head
    val groups = configuration.getLongSeq("veritask.groups").get
    timeSinceLastRequest > groups(user.group) || user.name == "testUser"
  }

  def updateTaskAttributes(
    task: Task, taskset: Taskset, link: Link): Future[Task] = {

    val subQueryString = taskset.subjectAttributesQuery.map(_.replaceAll(
      "\\{\\{\\s*linkSubject\\s*\\}\\}", "<" + link.linkSubject + ">"
    ))
    val objQueryString = taskset.objectAttributesQuery.map(_.replaceAll(
      "\\{\\{\\s*linkObject\\s*\\}\\}", "<" + link.linkObject + ">"
    ))
    for {
      subAttributes <- queryAttribute(taskset.subjectEndpoint, subQueryString)
      objAttributes <- queryAttribute(taskset.objectEndpoint, objQueryString)
      updatedTask = task.copy(
        subjectAttributes = subAttributes,
        objectAttributes = objAttributes)
      savedTask <- taskRepo.save(updatedTask)
    } yield savedTask
  }

  def queryAttribute(
    endpointOpt: Option[String],
    queryStringOpt: Option[String]): Future[Option[Map[String, String]]] = {

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
}
