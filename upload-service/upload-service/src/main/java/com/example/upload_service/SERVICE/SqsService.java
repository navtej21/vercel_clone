package com.example.upload_service.SERVICE;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
public class SqsService {


    private  final SqsClient sqsClient;
    private  final String queueUrl;


    /*
     now we are going to implement a messaging queue service where when we deploy
     project into the aws s3 the deployment id is passed to the messaging queue
     service for the heavy duty work to be done.
     */

    public SqsService(
            @Value("${aws.region}") String region,
            @Value("${aws.accessKeyId}") String accessKeyId,
            @Value("${aws.secretAccessKey}")String secretAccessKey,
            @Value("${aws.sqs.queueUrl}") String queueUrl){
        this.queueUrl = queueUrl;
        this.sqsClient = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build();

    }



    public String sendMessage(String messageBody){
        SendMessageRequest sendMsgRequest=SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build();
        SendMessageResponse response=sqsClient.sendMessage(sendMsgRequest);
        System.out.println("Message sent to SQS.ID"+response.messageId());
        return response.messageId();
    }
}
