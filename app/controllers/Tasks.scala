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
    jsonldReader.read(new StringReader(request.body), "") match {
      case Success(v) => taskService.bulkCreate(v.triples.toStream.map(i => Task.create(i))) map {
        case Right(success) => Ok
        case Left(err) => BadRequest(err)
      }
      case Failure(e) => Future.successful(BadRequest(e.getMessage))
    }
  }
}