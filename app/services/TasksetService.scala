package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import models.Taskset
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

@ImplementedBy(classOf[TasksetMongoService])
trait TasksetService extends CRUDService[Taskset, String]

import play.modules.reactivemongo.json.collection._

class TasksetMongoService @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends MongoCRUDService[Taskset, String] with TasksetService with ReactiveMongoComponents with MongoController {
  override val collection: JSONCollection = db.collection("Taskset")
}
