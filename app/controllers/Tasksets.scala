package controllers

import java.util.UUID
import javax.inject.Inject

import models.Taskset
import services.TasksetService

class Tasksets @Inject()(TasksetService: TasksetService)
    extends CRUDController[Taskset, UUID](TasksetService)(String => routes.Application.index)