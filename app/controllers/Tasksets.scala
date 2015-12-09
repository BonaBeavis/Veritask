package controllers

import javax.inject.Inject

import models.Taskset
import services.TasksetService

class Tasksets @Inject()(TasksetService: TasksetService)
  extends CRUDController[Taskset, String](TasksetService)(String => routes.Application.index)