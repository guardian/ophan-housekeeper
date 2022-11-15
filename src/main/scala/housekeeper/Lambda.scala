package housekeeper

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import play.api.libs.json.{JsError, JsSuccess, Json}
import software.amazon.awssdk.services.sns.model.PublishRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

object Lambda extends Logging {

  private val alertDeletion = new AlertDeletion(Dynamo.scanamo, "ophan-alerts")

  def handleBounce(bounceNotification: BounceNotification): Future[_] = {
    val bounce = bounceNotification.bounce
    logger.info(Map(
      "bounce.type" -> bounce.bounceType,
      "bounce.subtype" -> bounce.bounceSubType,
      "bounce.permanent" -> bounce.isPermanent,
      "bounce.mail.source" -> bounceNotification.senderOfBouncedEmail,
      "bounce.bouncedEmailAddresses" -> bounceNotification.bouncedAddresses.asJava
    ), bounceNotification.bounceSummary)

    if (bounce.isPermanent) {
      val deletionF = Future.traverse(bounceNotification.bouncedAddresses)(alertDeletion.deleteAllAlertsForEmailAddress)
      val notificationF = alertDevsIfEmailWasSuppressed(bounceNotification)
      for { _ <- deletionF; _ <- notificationF } yield ()
    } else Future.successful(())
  }

  private def alertDevsIfEmailWasSuppressed(bounceNotification: BounceNotification): Future[_] = {
    if (bounceNotification.bounce.isOnSuppressionList) {
      val arn = sys.env("PermanentEmailBounceTopicArn")
      logger.info(s"Sending an SNS alert to $arn")
      Future(AWS.SNS.publish(PublishRequest.builder().message(bounceNotification.bounceSummary).topicArn(arn).build()).get())
    } else Future.successful(())
  }

  /*
     * Logic handler
     */
  def go(message: String): Future[_] = {
    logger.info(Map(
      "notification.message" -> message
    ), s"Running with notification message")

    Json.parse(message).validate[BounceNotification] match {
      case JsSuccess(bounceNotification, _) => handleBounce(bounceNotification)
      case fail: JsError  =>
        logger.error(Map("notification.message" -> message),"Couldn't parse message as BounceNotification",fail)
        Future.successful(())
    }
  }

  /*
   * Lambda's entry point
   */
  def handler(lambdaInput: SNSEvent, context: Context): Unit = {
    val rawBounceNotifications = lambdaInput.getRecords.asScala.map(_.getSNS.getMessage).toSeq
    Await.ready(Future.traverse(rawBounceNotifications)(go), 30 seconds)
  }

}

