package com.example.deploy_service.SERVICE;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SqsPollerService {

    private final String queueUrl;
    private final SqsClient sqsClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final S3DownloadService s3DownloadService;

    public SqsPollerService(
            @Value("${aws.region}") String region,
            @Value("${aws.accessKeyId}") String accessKeyId,
            @Value("${aws.secretAccessKey}") String secretAccessKey,
            @Value("${aws.sqs.queueUrl}") String queueUrl, S3DownloadService s3DownloadService) {
        this.queueUrl = queueUrl;
        this.s3DownloadService = s3DownloadService;
        this.sqsClient = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build();
    }

    // running this on its own dedicated thread so the infinite loop never
    // blocks Spring's main startup thread
    @PostConstruct
    public void runForever() {
        executorService.submit(this::pollLoop);
    }

    public void pollLoop() {
        System.out.println("I have started");

        while (true) {
            try {
                ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                        .maxNumberOfMessages(1)
                        .queueUrl(queueUrl)
                        .waitTimeSeconds(20)
                        .build();

                ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(receiveMessageRequest);
                List<Message> messages = receiveMessageResponse.messages();

                if (messages.isEmpty()) {
                    continue; // nothing arrived within the 20s long-poll window — loop and try again
                }

                for (Message message : messages) {
                    String deploymentId=message.body();


                    String prefix="output/"+deploymentId;
                    String localDir="./output/"+deploymentId;


                    s3DownloadService.downloadDirectory(prefix,localDir);

                    sqsClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build());

                    System.out.println("Deleted from queue: " + message.messageId());
                }

            } catch (Exception e) {
                // caught HERE, inside the loop — one bad iteration logs and moves on,
                // it does not kill the entire polling loop forever
                System.out.println("Error while polling: " + e.getMessage());
            }
        }
    }
}