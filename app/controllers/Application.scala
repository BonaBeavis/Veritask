package controllers

import javax.inject.{Inject, Singleton}

import org.slf4j.{Logger, LoggerFactory}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
/**
  * Instead of declaring an object of Application as per the template project, we must declare a class given that
  * the application context is going to be responsible for creating it and wiring it up with the UUID generator service.
  *
  */
@Singleton
class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Application])

  def index = Action {
    logger.info("Serving main page...")
    Ok(views.html.index())
  }


}
