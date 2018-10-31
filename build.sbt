name := "housekeeper"

organization := "com.gu"

description:= "Housekeeping for Ophan"

version := "1.0"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.2",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.439",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.7", // https://app.snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-72451
  "com.typesafe.play" %% "play-json" % "2.6.10",
  "com.gu" %% "scanamo" % "1.0.0-M8",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test"

)

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}