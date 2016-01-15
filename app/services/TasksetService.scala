package services

import java.util.UUID
import javax.inject.Inject

import com.google.inject.ImplementedBy
import models.Taskset
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

@ImplementedBy(classOf[TasksetMongoService])
trait TasksetService extends CRUDService[Taskset, UUID]

import play.modules.reactivemongo.json.collection._

class TasksetMongoService @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends MongoCRUDService[Taskset, UUID] with TasksetService with ReactiveMongoComponents with MongoController {
  override val collection: JSONCollection = db.collection("Taskset")
}
