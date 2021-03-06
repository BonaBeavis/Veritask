package controllers

import java.util.UUID
import javax.inject.Inject

import config.ConfigBanana
import models.{User, Validation, Verification, VerificationDump}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsNull, Json}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.{Action, BodyParsers, Controller}
import services.{SimpleValidator, _}

import scala.concurrent.Future

/** Handles everything after the widget transmits a verification. Adds the
  * verification to the database. Handles the validation of the verification and
  * dumps the verification to a spqrql endpoint.
  */
class Verifications @Inject() (
                                val verificationRepo: VerificationMongoRepo,
                                val userRepo: UserMongoRepo,
                                val validator: SimpleValidator,
                                val messagesApi: MessagesApi,
                                val ws: WSClient,
                                val configuration: play.api.Configuration)
  extends Controller with I18nSupport with ConfigBanana {

  /** Prcesses a verification.
    *
    * @return a JSON boolean wrapped in a request. True if the user verified
    *         correctly, false if not, undefined if not enough verifications of
    *         this task exist to determine the correctness.
    */
  def processVerificationPost() = Action.async(BodyParsers.parse.json) {
    request =>
    val verification = request.body.validate[Verification]
    verification.fold(
      errors => {
        Future(BadRequest(
          Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      verification => {
        for {
          veri <- verificationRepo.save(verification)
          vali <- validator.validate(veri)
          user <- addValidation(verification.verifier_id, vali)
        } yield Ok(Json.toJson(vali.value))
      }
    )
  }

  /** Adds a validation to a user instance
    *
    * @param userID UUID of the user
    * @param validation validation to add
    *
    * @return Future of the user with the validation added
    */
  def addValidation(userID: UUID, validation: Validation): Future[User] = {
    userRepo.findById(userID) flatMap {
      case Some(u) => userRepo.save(
        u.copy(validations = validation :: u.validations))
      case None => throw new Exception("Non existing user validated")
    }
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
        verification.verifier_id.toString,
        link.copy(estimation = Some(estimation)),
        verification.value
      ).toPG.graph, "").get
    for {
      test <- test
      req <- request.withHeaders(
        "Content-Type" -> "text/turtle").withMethod("POST").post(test)
    } yield "Done"
  }
}
