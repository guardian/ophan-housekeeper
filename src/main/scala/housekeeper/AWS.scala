package housekeeper

import software.amazon.awssdk.auth.credentials.*
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.core.client.builder.SdkAsyncClientBuilder
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.Region.EU_WEST_1
import software.amazon.awssdk.services.dynamodb.{DynamoDbAsyncClient, DynamoDbAsyncClientBuilder}
import software.amazon.awssdk.services.sns.{SnsAsyncClient, SnsAsyncClientBuilder}

object AWS {
  val region: Region = EU_WEST_1

  def credentialsForDevAndProd(devProfile: String, prodCreds: AwsCredentialsProvider): AwsCredentialsProviderChain =
    AwsCredentialsProviderChain.of(prodCreds, ProfileCredentialsProvider.builder().profileName(devProfile).build())

  lazy val credentials: AwsCredentialsProvider =
    credentialsForDevAndProd("ophan", EnvironmentVariableCredentialsProvider.create())

  def build[T, B <: AwsClientBuilder[B, T] with SdkAsyncClientBuilder[B, T]](builder: B): T =
    builder.credentialsProvider(credentials).region(region)
      .httpClientBuilder(NettyNioAsyncHttpClient.builder()) // removing this causes failure after sbt-assembly
      .build()

  val SNS = build[SnsAsyncClient, SnsAsyncClientBuilder](SnsAsyncClient.builder())
  val dynamoDb = build[DynamoDbAsyncClient, DynamoDbAsyncClientBuilder](DynamoDbAsyncClient.builder())
}
