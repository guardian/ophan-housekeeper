name := "housekeeper"

organization := "com.gu"

description:= "Housekeeping for Ophan"

version := "1.0"

scalaVersion := "2.13.8"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

val scanamoVersion = "1.0.0-M23"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.0.1",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.32", //  log4j-over-slf4j provides `org.apache.log4j.MDC`, which is dynamically loaded by the Lambda runtime

  "ch.qos.logback" % "logback-classic" % "1.2.10",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.5.1", // So many Snyk warnings
  "com.typesafe.play" %% "play-json" % "2.10.0-RC7",
  "org.scanamo" %% "scanamo" % scanamoVersion,
  "org.scanamo" %% "scanamo-testkit" % scanamoVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.11" % Test
) ++ Seq("dynamodb", "sns", "url-connection-client").map(artifact => "software.amazon.awssdk" % artifact % "2.17.295")

enablePlugins(RiffRaffArtifact, BuildInfoPlugin)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")

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


buildInfoPackage := "housekeeper"
buildInfoKeys := {
  val buildInfo = com.gu.riffraff.artifact.BuildInfo(baseDirectory.value)
  Seq[BuildInfoKey](
    "buildNumber" -> buildInfo.buildIdentifier,
    "gitCommitId" -> buildInfo.revision,
    "buildTime" -> System.currentTimeMillis
  )
}