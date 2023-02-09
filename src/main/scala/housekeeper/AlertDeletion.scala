package housekeeper

import cats.implicits.*
import housekeeper.Dynamo.OphanAlert
import org.scanamo.*
import org.scanamo.generic.auto.*
import org.scanamo.syntax.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Dynamo extends Logging {

  val scanamo: ScanamoAsync = ScanamoAsync(AWS.dynamoDb)

  case class OphanAlert(
                         email: String,
                         alertName: String
                       ) {
    val primaryKey = "email" === email and "alertName" === alertName
  }
}

class AlertDeletion(scanamo: ScanamoAsync, tableName: String) extends Logging {
  logger.info(s"Table name = $tableName")

  val table = Table[OphanAlert](tableName)

  def deleteAllAlertsForEmailAddress(email: String): Future[Unit] = {
    val baseContext = Map("alertsToDelete.emailAddress" -> email)
    logger.info(baseContext, s"About to search for alerts to delete for '$email'")
    scanamo.exec(for {
      results <- table.query("email" === email)
      alertsForEmail = results.flatMap(_.toOption)
      _ = logger.info(baseContext ++ Map(
        "alertsToDelete.found.count" -> alertsForEmail.size,
      ), s"About to delete ${alertsForEmail.size} alerts for '$email'")
      _ <- alertsForEmail.traverse(alert => table.delete(alert.primaryKey))
    } yield {
      logger.info(baseContext, s"...successfully deleted ${alertsForEmail.size} alerts for '$email'")
    })
  }

}
