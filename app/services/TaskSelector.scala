//package services
//
//import com.google.inject.Inject
//import models.{Task, User}
//
//import scala.concurrent.Future
//import play.api.Play.current
//import play.modules.reactivemongo.ReactiveMongoApi
///**
//  * Created by beavis on 17.06.16.
//  */
//trait TaskSelector {
//  def selectTask(user: User): Future[Task]
//}
//
//class SimpleTaskSelector @Inject()(
//                                    val verificationRepo: VerificationRepo
//                                  ) extends TaskSelector {
//
//  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
//
//  def selectTask(user: User): Future[Task] = {
//    reactiveMongoApi.db.j
//  }
//}
