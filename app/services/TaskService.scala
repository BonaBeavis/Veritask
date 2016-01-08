package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import models.Task
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

@ImplementedBy(classOf[TaskMongoService])
trait TaskService extends CRUDService[Task, String]

import play.modules.reactivemongo.json.collection._

class TaskMongoService @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends MongoCRUDService[Task, String] with TaskService with ReactiveMongoComponents with MongoController {
  override val collection: JSONCollection = db.collection("Task")
}
