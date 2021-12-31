name := "housekeeper"

organization := "com.gu"

description:= "Housekeeping for Ophan"

version := "1.0"

scalaVersion := "2.13.7"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

val awsSdkVersion = "1.12.122"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.0.1",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.32", //  log4j-over-slf4j provides `org.apache.log4j.MDC`, which is dynamically loaded by the Lambda runtime
  "ch.qos.logback" % "logback-classic" % "1.2.7",

  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-sns" % awsSdkVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.5.1", // So many Snyk warnings
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "org.scanamo" %% "scanamo" % "1.0.0-M11",
  "org.scanamo" %% "scanamo-testkit" % "1.0.0-M11" % Test,
  "org.scalatest" %% "scalatest" % "3.2.10" % Test
)

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
buildInfoKeys := Seq[BuildInfoKey](
  "buildNumber" -> Option(System.getenv("BUILD_NUMBER")).getOrElse("DEV"),
  // so this next one is constant to avoid it always recompiling on dev machines.
  // we only really care about build time on teamcity, when a constant based on when
  // it was loaded is just fine
  "buildTime" -> System.currentTimeMillis,
  "gitCommitId"-> Option(System.getenv("BUILD_VCS_NUMBER")).getOrElse("DEV")
)