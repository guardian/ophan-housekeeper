import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { GuStack } from "@guardian/cdk/lib/constructs/core";
import type { App } from "aws-cdk-lib";
import {GuLambdaFunction} from "@guardian/cdk/lib/constructs/lambda";
import {Runtime} from "aws-cdk-lib/aws-lambda";
import {SnsEventSource} from "aws-cdk-lib/aws-lambda-event-sources";
import {Topic} from "aws-cdk-lib/aws-sns";

export class Housekeeper extends GuStack {

  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);

    const bounceSNSTopic = new Topic(this, "BounceSNSTopic", {
      displayName: "SNS topic alerted when AWS SES emails bounce (eg from Ophan Dashboard alerts, or Airflow)",
      topicName: "ses-email-bounce-notifications-for-housekeeper"
    })
    this.overrideLogicalId(bounceSNSTopic, {logicalId: "BounceSNSTopic", reason: "We don't want AWS to delete the old topic and create a new one."})

    const permanentEmailBounceTopic = new Topic(this, "PermanentEmailBounceTopic", {
      displayName: "Threat to email-sending ability of Ophan's AWS account",
    })
    this.overrideLogicalId(permanentEmailBounceTopic, {logicalId: "PermanentEmailBounceTopic", reason: "We don't want AWS to delete the old topic and create a new one."})

    const app = "housekeeper"

    const lambda = new GuLambdaFunction(this, "housekeeper", {
      app,
      fileName: "ophan-dist/ophan/PROD/housekeeper/housekeeper.jar",
      runtime: Runtime.JAVA_11,
      handler: "housekeeper.Lambda::handler",
      environment: {PermanentEmailBounceTopicArn: permanentEmailBounceTopic.topicArn}
    })
    lambda.addEventSource(new SnsEventSource(bounceSNSTopic));
  }
}