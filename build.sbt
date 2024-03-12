name := "housekeeper"

organization := "com.gu"

description:= "Housekeeping for Ophan"

version := "1.0"

scalaVersion := "3.2.2"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
)

val scanamoVersion = "1.0.0-M26"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.3",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.4",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.4",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.32", //  log4j-over-slf4j provides `org.apache.log4j.MDC`, which is dynamically loaded by the Lambda runtime

  "ch.qos.logback" % "logback-classic" % "1.2.13",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.4", // So many Snyk warnings
  "com.typesafe.play" %% "play-json" % "2.10.4",
  "org.scanamo" %% "scanamo" % scanamoVersion,
  "org.scanamo" %% "scanamo-testkit" % scanamoVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.16" % Test
) ++ Seq("dynamodb", "sns", "url-connection-client").map(artifact => "software.amazon.awssdk" % artifact % "2.25.6")

enablePlugins(BuildInfoPlugin)

assembly / assemblyOutputPath  := file(s"target/${name.value}.jar")
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

startDynamoDBLocal := startDynamoDBLocal.dependsOn(Test / compile).value

dynamoDBLocalPort := 8042

inConfig(Test)(Seq(
  test := (Test / test).dependsOn(startDynamoDBLocal).value,
  testOnly := (Test / testOnly).dependsOn(startDynamoDBLocal).evaluated,
  testQuick := (Test / testQuick).dependsOn(startDynamoDBLocal).evaluated,
  testOptions += dynamoDBLocalTestCleanup.value
))

def env(propName: String): Option[String] = sys.env.get(propName).filter(_.trim.nonEmpty)

buildInfoPackage := "housekeeper"
buildInfoKeys ++= Seq[BuildInfoKey](
  name,
  scalaVersion,
  sbtVersion,

  // copied from https://github.com/guardian/sbt-riffraff-artifact/blob/e6f5e62d8f776b1004f72ed1ea415328fa43ed31/src/main/scala/com/gu/riffraff/artifact/BuildInfo.scala
  BuildInfoKey.sbtbuildinfoConstantEntry("buildNumber", env("GITHUB_RUN_NUMBER")),
  BuildInfoKey.sbtbuildinfoConstantEntry("buildTime", System.currentTimeMillis),
  BuildInfoKey.sbtbuildinfoConstantEntry("gitCommitId", env("GITHUB_SHA")),

  BuildInfoKey.sbtbuildinfoConstantEntry(
    "branch",
    env("GITHUB_HEAD_REF")
      .orElse(env("GITHUB_REF"))
      .orElse(Some("unknown-branch"))
      .get
      .stripPrefix("refs/heads/")),
)