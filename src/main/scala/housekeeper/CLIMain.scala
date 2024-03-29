package housekeeper

import housekeeper.Lambda.go

import java.io.File
import java.nio.file.{Files, Paths}
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import concurrent.duration.DurationInt

/**
 * This class isn't used in production, it's only for local testing.
 *
 * It's separate from the Lambda class because adding the scala.App
 * trait pull in scala.DelayedInit, which seems to lead to
 * java.lang.NullPointerException errors when invoked as a Lambda -
 * vals are not initialised by the time we come to service the request?!
 */

@main def cliMain(messageFile: String) = {
  val message = Files.readString(Paths.get(messageFile))
  Await.ready(Lambda.go(message), 30 seconds)
}

