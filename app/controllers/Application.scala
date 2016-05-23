package controllers

import javax.inject.{Inject, Singleton}

import org.slf4j.{Logger, LoggerFactory}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.Jsonp
import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.twirl.api.Html

/**
  * Instead of declaring an object of Application as per the template project, we must declare a class given that
  * the application context is going to be responsible for creating it and wiring it up with the UUID generator service.
  *
  */
@Singleton
class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Application])

  def index = Action {
    Ok(views.html.index())
  }

  def template(callback: String) = Action {
    val json = Json.obj("html" -> Json.toJson(views.html.widget.render().toString))
    Ok(Jsonp(callback, json))
  }

  def getTemplate = Action {
    Ok("r")
  }

  def widgetHTML = Action {
    Ok(views.html.widget())
  }

//  def getTask = Action.async {
//    TaskDao.findRandom() map {
//      case Some(task) => Ok(Json.toJson(task))
//      case None => Ok("nothing found")
//    }
////    val template = views.html.template(task)
////    val json = Json.obj("html" -> Json.toJson(views.html.template(task).render().toString))
////    Ok(json)
//  }

  def widget = Action { request =>
    Ok(views.js.widget.render(request))
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.Application.template,
        routes.javascript.Assets.versioned
      )
    ).as("text/javascript")
  }
}
