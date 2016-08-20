package controllers

import javax.inject.{Inject, Singleton}

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter

/**
  * Instead of declaring an object of Application as per the template project, we must declare a class given that
  * the application context is going to be responsible for creating it and wiring it up with the UUID generator service.
  *
  */
@Singleton
class Application @Inject()(val messagesApi: MessagesApi)
  extends Controller with I18nSupport {

  def index = Action {
    Ok(views.html.index())
  }

  def widgetHTML = Action {
    Ok(views.html.widget())
  }

  def widget = Action { request =>
    Ok(views.js.widget.render(request))
  }
    def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.Tasks.requestTaskEval,
        routes.javascript.Assets.versioned
      )
    ).as("text/javascript")
  }
}
