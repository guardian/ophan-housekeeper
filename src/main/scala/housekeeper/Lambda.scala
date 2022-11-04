package housekeeper

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import play.api.libs.json.{JsError, JsSuccess, Json}
import software.amazon.awssdk.services.sns.model.PublishRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

object Lambda extends Logging {

  private val alertDeletion = new AlertDeletion(Dynamo.scanamo, "ophan-alerts")

  def handleBounce(bounceNotification: BounceNotification): Future[_] = {
    val bounce = bounceNotification.bounce

    val bouncedAddresses = bounce.bouncedEmailAddresses.toSeq.sorted

    val senderOfBouncedEmail = bounceNotification.mail.source
    val bounceSummary = s"$senderOfBouncedEmail sent a ${bounce.bounceType} bounced message to ${bouncedAddresses.mkString(",")}"
    logger.info(Map(
      "bounce.type" -> bounce.bounceType,
      "bounce.subtype" -> bounce.bounceSubType,
      "bounce.permanent" -> bounce.isPermanent,
      "bounce.mail.source" -> senderOfBouncedEmail,
      "bounce.bouncedEmailAddresses" -> bouncedAddresses.asJava
    ), bounceSummary)

    if (bounce.isPermanent) {
      Future.traverse(bouncedAddresses)(alertDeletion.deleteAllAlertsForEmailAddress)
      if (bounce.isOnSuppressionList) {
        val arn = sys.env("PermanentEmailBounceTopicArn")
        logger.info(s"Sending an SNS alert to $arn")
        Future(AWS.SNS.publish(PublishRequest.builder().message(bounceSummary).topicArn(arn).build()).get())
      } else {
        Future.successful(Unit)
      }
    } else {
      Future.successful(Unit)
    }
  }

  /*
   * Logic handler
   */
  def go(message: String): Unit = {
    logger.info(Map(
      "notification.message" -> message
    ), s"Running with notification message")

    Json.parse(message).validate[BounceNotification] match {
      case JsSuccess(bounceNotification, _) => {
        handleBounce(bounceNotification)
      }
      case fail: JsError  =>
        logger.error(Map("notification.message" -> message),"Couldn't parse message as BounceNotification",fail)
    }
  }

  /*
   * Lambda's entry point
   */
  def handler(lambdaInput: SNSEvent, context: Context): Unit = {
    lambdaInput.getRecords.asScala.map(_.getSNS.getMessage).toList.headOption foreach go
  }

}

