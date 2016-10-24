package controllers

import javax.inject.{Inject, Singleton}

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter

/** Serves index pages for veritask management frontend and widget assets.
  *
  */
@Singleton
class Application @Inject()(val messagesApi: MessagesApi)
  extends Controller with I18nSupport {

  // Index page for veritask
  def index = Action {
    Ok(views.html.index())
  }

  // Renders and serves the widgets javascript
  def widget = Action { request =>
    Ok(views.js.widget.render(request))
  }

  // Serves the widgets HTML frame
  def widgetHTML = Action {
    Ok(views.html.widget())
  }

  // Serves javscript routes object for the widget
  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.Tasks.requestTaskEval,
        routes.javascript.Assets.versioned
      )
    ).as("text/javascript")
  }
}
