package controllers

import java.io.StringReader
import javax.inject.Inject

import config.ConfigBanana
import models.Task
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import services.TaskService

import scala.concurrent.Future
import scala.util.{Failure, Success}

class Tasks @Inject()(taskService: TaskService)
  extends CRUDController[Task, String](taskService)(String => routes.Application.index)
  with ConfigBanana {

  def createBulk = Action.async(parse.text) { request =>
    import ops._
    Future {
      val graph = jsonldReader.read(new StringReader(request.body), "arg")
      graph match {
        case Success(v) => v.triples map (triple => taskService.create(Task.create(triple)))
        case Failure(e) => Future.successful()
      }
    }.flatMap {
      case Success(v) => Future.successful(BadRequest)
      case Failure(e) => Future.successful(BadRequest)
    }
  }
}