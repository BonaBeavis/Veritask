package controllers

import java.net.URL
import java.util.UUID
import javax.inject.Inject

import config.ConfigBanana
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsNull, JsString, JsValue, Json}
import play.api.mvc._
import services._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class UserRequest[A](val user: User, request: Request[A])
  extends WrappedRequest[A](request)

class Tasks @Inject() (
                        val tasksetRepo: TasksetMongoRepo,
                        val taskRepo: TaskMongoRepo,
                        val linkRepo: LinkMongoRepo,
                        val userRepo: UserMongoRepo,
                        val evalDataRepo: EvalDataRepo,
                        val messagesApi: MessagesApi,
                        val configuration: play.api.Configuration)
  extends Controller
  with I18nSupport
  with ConfigBanana {

  def UserAction(name: String) = new ActionRefiner[Request, UserRequest] {
    def refine[A](input: Request[A]) =
      userRepo.search("name", name) map {
        case users: Traversable[User] if users.nonEmpty
          => Right(new UserRequest(users.head, input))
        case _ =>
          val user = User(UUID.randomUUID(), name)
          userRepo.save(user)
          Right(new UserRequest(user, input))
      }
  }

    def getUser(name: String): Future[User] = {
    userRepo.search("name", name) flatMap {
      case users: Traversable[User] if users.nonEmpty => Future(users.head)
      case _ => userRepo.save(User(UUID.randomUUID(), name))
    }
  }

  def requestTaskEval(name: String, taskset: Option[String], ability: Boolean) =
    (Action andThen UserAction(name)).async { request =>
      getEvalData(request.user).map(isTurn(request.user,_, ability)) flatMap {
        case true =>  getTask(request.user, taskset) flatMap {
          case Some(t) =>
            for {
              link <- linkRepo.findById(t.link_id)
              taskset <- tasksetRepo.findById(t.taskset)
            } yield {
              Ok(Json.toJson(
                Widget(request.user._id, link.get, t, taskset.get.template)
              ))
            }
          case None => Future.successful(Ok(Json.toJson(JsNull)))
        }
        case false => Future.successful(Ok(Json.toJson(JsNull)))
      }
    }

  def isTurn(
                      user: User,
                      evalData: EvalData,
                      ability: Boolean
                    ): Boolean = {
    val timeSinceLastRequest = user.validations.nonEmpty match {
      case true => System.currentTimeMillis() - user.validations.head.time
      case false => Long.MaxValue
    }
    val isForAbility = evalData.group == 2 && ability // Magic number two
    val isTimed = evalData.group == 1 &&
        timeSinceLastRequest > evalData.taskDelay &&
        !ability
    isForAbility || isTimed
  }

  def getEvalData(user: User): Future[EvalData] = {
    val delayGroups = configuration.getLongSeq("veritask.delays").get
    evalDataRepo.search("user_id", user._id.toString) flatMap {
      case eD: Traversable[EvalData] if eD.nonEmpty =>
        val evalData = eD.head
        val stampedEvalData = evalData.copy(
          timeStamps = System.currentTimeMillis() :: evalData.timeStamps)
        evalDataRepo.save(stampedEvalData)
      case _ =>
        evalDataRepo.save(
          EvalData(
            UUID.randomUUID(),
            user._id,
            scala.util.Random.nextInt(3),
            delayGroups(scala.util.Random.nextInt(delayGroups.length)),
            List(System.currentTimeMillis())
          )
        )
    }
  }

  def requestTask(name: String, taskset: Option[String]) =
    (Action andThen UserAction(name)).async { request =>
      getTask(request.user, taskset) flatMap {
        case Some(t) =>
          for {
            link <- linkRepo.findById(t.link_id)
            taskset <- tasksetRepo.findById(t.taskset)
          } yield {
            Ok(Json.toJson(
              Widget(request.user._id, link.get, t, taskset.get.template)
            ))
          }
        case None => Future.successful(Ok(Json.toJson(JsNull)))
      }
    }

  def getTask(
                     user: User,
                     taskset: Option[String]
                   ): Future[Option[Task]] = {
    taskRepo.selectTaskToVerify(taskset, user.validations.map(_.task_id)) flatMap {
      case Some(task) => updateTaskAttributes(task) flatMap {
        case Some(updatedTask) => Future.successful(Some(updatedTask))
        case None => getTask(user, taskset)
      }
      case None => Future.successful(None)
    }
  }

  def updateTaskAttributes(task: Task): Future[Option[Task]] = {
    val updatedTask = for {
      taskset <- tasksetRepo.findById(task.taskset)
      link <- linkRepo.findById(task.link_id)
      subQueryString = taskset.get.subjectAttributesQuery.map(_.replaceAll(
        "\\{\\{\\s*linkSubjectURI\\s*\\}\\}", "<" + link.get.linkSubject + ">"
      ))
      objQueryString = taskset.get.objectAttributesQuery.map(_.replaceAll(
        "\\{\\{\\s*linkObjectURI\\s*\\}\\}", "<" + link.get.linkObject + ">"
      ))
      subAttributes <- queryAttribute(
        taskset.get.subjectEndpoint,
        subQueryString,
        task.subjectAttributes)
      objAttributes <- queryAttribute(
        taskset.get.objectEndpoint,
        objQueryString,
        task.objectAttributes)
    } yield task.copy(
      subjectAttributes = subAttributes,
      objectAttributes = objAttributes
    )
    updatedTask map {
      case t:Task if t.objectAttributes.nonEmpty && t.subjectAttributes.nonEmpty
        => Some(t)
      case _ =>
        taskRepo.delete(task._id)
        None
    }
  }

  def queryAttribute(
    endpointOpt: Option[String],
    queryStringOpt: Option[String],
    attribute: Option[Map[String, String]]): Future[Option[Map[String, String]]] = {

    import ops._
    import sparqlHttp.sparqlEngineSyntax._
    import sparqlOps._

    val result = (endpointOpt, queryStringOpt, attribute) match {
      case (Some(endpoint), Some(queryString), None) =>
        val endpointURL = new URL(endpoint)
        for {
          query <- parseSelect(queryString)
          solutions <- endpointURL.executeSelect(query, Map())
        } yield {
          solutions.hasNext match {
            case true =>
              val solution = solutions.iterator.next
              Some(solution.vars.map(a => a -> solution.get(a).toString).toMap)
            case false => None
          }
        }
      case _ => Try(Some(Map(("", ""))))
    }
    result match {
      case Success(attributes) => Future.successful(attributes)
      case Failure(t) => Future.failed(t)
    }
  }
}
