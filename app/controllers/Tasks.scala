package controllers

import javax.inject.Inject

import config.ConfigBanana
import models.Task
import services.TaskService

class Tasks @Inject()(taskService: TaskService)
  extends CRUDController[Task, String](taskService)(String => routes.Application.index)
  with ConfigBanana {

  //  def createBulk = Action.async(parse.text) { request =>
  //    import ops._
  //    jsonldReader.read(new StringReader(request.body), "") match {
  //      case Success(v) => taskService.bulkCreate(v.triples.toStream.map(i => Task.create(i))) map {
  //        case Right(success) => Ok
  //        case Left(err) => BadRequest(err)
  //      }
  //      case Failure(e) => Future.successful(BadRequest(e.getMessage))
  //    }
  //  }
}