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

// Request with added user instance
class UserRequest[A](val user: User, request: Request[A])
  extends WrappedRequest[A](request)

/** Serves, updates, selects tasks to verifiy. During evaluation this controller
  * additionally collects data about the users.
  */
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

  /** Adds user instance to request, creates new user instance if none with the
    * specified name exists.
    */
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

  /** Provides a task for a user. Optionally from a specific taskset.
    *
    * This function is a "override" for the requestTask function in context of
    * thesis's evaluation. The MonsterMinigame simply requests task every
    * x seconds and every time a player uses a ability. These requests are used
    * to track the play time of a player. Tasks are only served, when it is the
    * player's turn.
    *
    * @param name the player's name
    * @param taskset_id if provided, select a task from this taskset
    * @param ability was the task request from the MosterMinigame triggered by
    *                a ability
    */
  def requestWidgetTaskDataEval(name: String, taskset_id: Option[String], ability: Boolean) =
    (Action andThen UserAction(name)).async { request =>
      getEvalData(request.user).map(isTurn(request.user, _, ability)) flatMap {
        case true =>  getTask(request.user, taskset_id) flatMap {
          case Some(task) =>
            for {
              link <- linkRepo.findById(task.link_id)
              taskset <- tasksetRepo.findById(task.taskset_id)
            } yield {
              Ok(Json.toJson(
                WidgetTaskData(request.user._id, link.get, task, taskset.get.template)
              ))
            }
          case None => Future.successful(Ok(Json.toJson(JsNull)))
        }
        case false => Future.successful(Ok(Json.toJson(JsNull)))
      }
    }

  /** Checks if it users turn to get a task.
    *
    * This is a helper function for the evaluation.
    * Group 0: No Tasks, Group 1: Task every x seconds, Group 3: Solve task for
    * using ability.
    *
    * @param player the player's instance
    * @param evalData data about the player for the evaluation
    * @param ability was the task request from the MosterMinigame triggered by
    *                a ability
    */
  def isTurn(player: User, evalData: EvalData, ability: Boolean): Boolean = {
    val timeSinceLastRequest = player.validations.nonEmpty match {
      case true => System.currentTimeMillis() - player.validations.head.time
      case false => Long.MaxValue
    }
    val isForAbility = evalData.group == 2 && ability // Magic number two
    val isTimed = evalData.group == 1 &&
        timeSinceLastRequest > evalData.taskDelay * 1000 &&
        !ability
    isForAbility || isTimed
  }

  /** Gets and updates the evaluation data for the user. Creates new evalData
    * instance if none exists.
    *
    * @param user: the user's instance
    * @return A EvalData future
    */
  def getEvalData(user: User): Future[EvalData] = {
    // How many seconds should be between tasks
    val delayGroups = configuration.getLongSeq("veritask.delays").get
    // Time between request > inactiveThreshold and user counts as inactive
    val inactiveThreshold = configuration.getLong("veritask.inactiveThreshold").get
    evalDataRepo.search("user_id", user._id.toString) flatMap {
      case eD: Traversable[EvalData] if eD.nonEmpty =>
        val evalData = eD.head
        val now = System.currentTimeMillis()
        val timeSinceLastTimestamp = now - evalData.timeStamps.head
        val playPeriod = if (timeSinceLastTimestamp < inactiveThreshold) timeSinceLastTimestamp else 0L
        evalDataRepo.save(evalData.copy(
          timeStamps = now :: evalData.timeStamps,
          timePlayed = evalData.timePlayed + playPeriod
        ))
      case _ => user.name match {
        case "noTasks" =>
          evalDataRepo.save(
            EvalData(UUID.randomUUID(), user._id, 0, 20, List(System.currentTimeMillis()))
          )
        case "timedTasks" =>
          evalDataRepo.save(
            EvalData( UUID.randomUUID(), user._id, 1, 10, List(System.currentTimeMillis()))
          )
        case "abilityTasks" =>
          evalDataRepo.save(
            EvalData( UUID.randomUUID(), user._id, 2, 20, List(System.currentTimeMillis()))
          )
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
  }

  /** Provides a task for a user. Optionally from a specific taskset.
    *
    * @param name the player's name
    * @param taskset_id if provided, select a task from this taskset
    *
    * TODO: Get widgetdata in own function, eleminate getTask
    */
  def requestWidgetTaskData(name: String, taskset_id: Option[String]) =
    (Action andThen UserAction(name)).async { request =>
      getTask(request.user, taskset_id) flatMap {
        case Some(t) =>
          for {
            link <- linkRepo.findById(t.link_id)
            taskset <- tasksetRepo.findById(t.taskset_id)
          } yield {
            Ok(Json.toJson(
              WidgetTaskData(request.user._id, link.get, t, taskset.get.template)
            ))
          }
        case None => Future.successful(Ok(Json.toJson(JsNull)))
      }
    }

  /** Provides a task for a user. Optionally from a specific taskset.
    *
    */
  def getTask( user: User, taskset: Option[String] ): Future[Option[Task]] = {
    taskRepo.selectTaskToVerify(taskset, user.validations.map(_.task_id)) flatMap {
      case Some(task) => updateTaskAttributes(task)
      case None => Future.successful(None)
    }
  }

  /** Tries to fill the tasks attributes with data by executing the sparql
    * query, provided by the taskset, against the tasks subject and object. If
    * the query returns no data for either, deletes the task.
    *
    * @param task the task to update
    * @return A future of the updated task if successful. A future of a None if
    *         no attribute data was found.
    */
  def updateTaskAttributes(task: Task): Future[Option[Task]] = {
    val updatedTask = for {
      taskset <- tasksetRepo.findById(task.taskset_id)
      link <- linkRepo.findById(task.link_id)
      // Substitute the placeholder with the actual subject and object URI of the Task.
      subQueryString = taskset.get.subjectAttributesQuery.map(_.replaceAll(
        "\\{\\{\\s*linkSubjectURI\\s*\\}\\}", "<" + link.get.linkSubject + ">"
      ))
      objQueryString = taskset.get.objectAttributesQuery.map(_.replaceAll(
        "\\{\\{\\s*linkObjectURI\\s*\\}\\}", "<" + link.get.linkObject + ">"
      ))
      subAttributes <- queryAttributes(
        taskset.get.subjectEndpoint,
        subQueryString,
        task.subjectAttributes)
      objAttributes <- queryAttributes(
        taskset.get.objectEndpoint,
        objQueryString,
        task.objectAttributes)
    } yield task.copy(
      subjectAttributes = subAttributes,
      objectAttributes = objAttributes
    )
    updatedTask flatMap {
      case t:Task if t.objectAttributes.nonEmpty && t.subjectAttributes.nonEmpty
        => taskRepo.save(t) map(Some(_))
      case _ => Future.successful(None)
    }
  }

  /** Queries attributes, by executing a sparql query against an endpoint. This
    * will only happen if a endpiont and a query exists. Additionally attributes
    * should be None, to be sure that those attributes wont get queried multiple
    * times.
    *
    *
    * @param endpointOpt a sparql enpoint
    * @param queryStringOpt the query to execute
    * @param attributes attributes of the resource
    *
    * @return A map with empty strings, if endpoint or query are not defined.
    *
    * For example, the query:
    * {{{
    *   select distinct ?label ?title where {
    *     {{ linkObjectURI }} rdfs:label ?label.
    *     {{ linkObjectURI }} rdfs:titel ?titel
    *   }
    * }}}
    * Would return a map like (label => QUERIED_LABEL_VALUE, titel => QUERIED_TITEL_VALUE)
    */
  def queryAttributes(
                       endpointOpt: Option[String],
                       queryStringOpt: Option[String],
                       attributes: Option[Map[String, String]]
                     ): Future[Option[Map[String, String]]] = {

    import ops._
    import sparqlHttp.sparqlEngineSyntax._
    import sparqlOps._

    val result = (endpointOpt, queryStringOpt, attributes) match {
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
      case (Some(endpoint), Some(queryString), Some(attr)) => Try(Some(attr))
      case _ => Try(Some(Map(("", ""))))
    }
    result match {
      case Success(attributes) => Future.successful(attributes)
      case Failure(t) => Future.failed(t)
    }
  }
}
