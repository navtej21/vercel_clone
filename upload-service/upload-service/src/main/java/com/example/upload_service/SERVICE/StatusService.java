package com.example.upload_service.SERVICE;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

/*

// so this is to let the user know how the frontend artifacts are getting build stage by stage.
so here we will store in the map
 */
@Service
public class StatusService {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;


    public StatusService(
            @Value("${aws.accessKeyId}") String accessKey,
            @Value("${aws.secretAccessKey}") String secretAccess,
            @Value("${aws.region}") String region,
            @Value("${aws.dynamodb.table}") String dynamoTable
    ){

        this.tableName=dynamoTable;
        this.dynamoDbClient=DynamoDbClient.builder().region(Region.of(region)).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey,secretAccess))).build();
    }


    // we will write a function to update the status
    public void updateStatus(String deploymentId,String status){
        Map<String, AttributeValue> item=new HashMap<>();
        item.put("deploymentKey", AttributeValue.builder().s(deploymentId).build());
        item.put("status", AttributeValue.builder().s(status).build());
        PutItemRequest putItemRequest=PutItemRequest.builder().tableName(tableName).item(item).build();
        dynamoDbClient.putItem(putItemRequest);
        System.out.println("Status Updated"+deploymentId+":"+status);
    }
}
