import type { GuStackProps } from '@guardian/cdk/lib/constructs/core';
import { GuStack } from '@guardian/cdk/lib/constructs/core';
import { GuLambdaFunction } from '@guardian/cdk/lib/constructs/lambda';
import type { App } from 'aws-cdk-lib';
import { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Runtime } from 'aws-cdk-lib/aws-lambda';
import { SnsEventSource } from 'aws-cdk-lib/aws-lambda-event-sources';
import { Topic } from 'aws-cdk-lib/aws-sns';

export class Housekeeper extends GuStack {
	constructor(scope: App, id: string, props: GuStackProps) {
		super(scope, id, props);

		const bounceSNSTopic = new Topic(this, 'BounceSNSTopic', {
			displayName:
				'SNS topic alerted when AWS SES emails bounce (eg from Ophan Dashboard alerts, or Airflow)',
			topicName: 'ses-email-bounce-notifications-for-housekeeper',
		});
		this.overrideLogicalId(bounceSNSTopic, {
			logicalId: 'BounceSNSTopic',
			reason: "We don't want AWS to delete the old topic and create a new one.",
		});

		const permanentEmailBounceTopic = new Topic(
			this,
			'PermanentEmailBounceTopic',
			{
				displayName: "Threat to email-sending ability of Ophan's AWS account",
			},
		);
		this.overrideLogicalId(permanentEmailBounceTopic, {
			logicalId: 'PermanentEmailBounceTopic',
			reason: "We don't want AWS to delete the old topic and create a new one.",
		});

		const app = 'housekeeper';

		const loggingPolicy = new PolicyStatement();
		loggingPolicy.addResources('arn:aws:logs:*:*:*');
		loggingPolicy.addActions(
			'logs:CreateLogGroup',
			'logs:CreateLogStream',
			'logs:PutLogEvents',
		);

		//TODO: change the codebase to use Dev-ophan-alerts table when testing locally or in CODE or remove this table if not used
		const dynamodbPolicy = new PolicyStatement();
		dynamodbPolicy.addResources(
			`arn:aws:dynamodb:*:${this.account}:table/ophan-alerts`,
			`arn:aws:dynamodb:*:${this.account}:table/DEV-ophan-alerts`,
		);
		dynamodbPolicy.addActions('dynamodb:Query', 'dynamodb:DeleteItem');

		const lambda = new GuLambdaFunction(this, 'housekeeper', {
			app,
			fileName: 'housekeeper.jar',
			description: 'Housekeeping for Ophan',
			environment: {
				PermanentEmailBounceTopicArn: permanentEmailBounceTopic.topicArn,
			},
			handler: 'housekeeper.Lambda::handler',
			runtime: Runtime.JAVA_11,
		});
		lambda.addEventSource(new SnsEventSource(bounceSNSTopic));

		permanentEmailBounceTopic.grantPublish(lambda);
		lambda.addToRolePolicy(loggingPolicy);
		lambda.addToRolePolicy(dynamodbPolicy);
	}
}
