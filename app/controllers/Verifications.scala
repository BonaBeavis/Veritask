package controllers

import javax.inject.Inject

import config.ConfigBanana
import models.{Verification, VerificationDump}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, Json}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.{Action, BodyParsers, Controller}
import services.{SimpleValidator, _}

import scala.concurrent.Future

class Verifications @Inject() (
  val verificationRepo: VerificationRepo,
  val validator: SimpleValidator,
  val messagesApi: MessagesApi,
  val ws: WSClient,
  val configuration: play.api.Configuration)
  extends Controller with I18nSupport with ConfigBanana {

  def processVerificationPost() = Action.async(BodyParsers.parse.json) {
    request =>
    val verification = request.body.validate[Verification]
    verification.fold(
      errors => {
        Future(BadRequest(
          Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      verification => {
        val estimation = for {
          veri <- verificationRepo.save(verification)
          est <- validator.process(veri)
          throwaway <- dumpVerification(veri, est)
        } yield est
        estimation map {
          case a: Double if a > 0.5 => Ok(Json.toJson(true))
          case _ => Ok(Json.toJson(false))
        }
      }
    )
  }

  def dumpVerification(verification: Verification, estimation: Double) =  {
    val url = configuration.getString("triplestore.uri").get
    val request: WSRequest = ws.url(url)
    import ops._
    val test = for {
      link <- verificationRepo.getLink(verification)
    } yield turtleWriter.asString(
      VerificationDump(
        verification._id,
        verification.verifier.toString,
        link.copy(estimation = Some(estimation)),
        Some(true)
      ).toPG.graph, "").get
    for {
      test <- test
      req <- request.withHeaders(
        "Content-Type" -> "text/turtle").withMethod("POST").post(test)
    } yield "Done"
  }
}
