package com.example.request_handler_service.SERVICE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.net.URLConnection;

@Service
public class RequestService {


    private String bucketName;
    private String secretAccessKey;
    private String accessKeyId;
    private String region;
    private S3Client s3Client;

    public RequestService(
            @Value("${aws.accessKeyId}") String accessKeyId,
            @Value("${aws.secretAccessKey}") String secretAccessKey,
            @Value("${aws.region}") String region,
            @Value("${aws.s3.bucket}") String bucketName
    ){
        this.bucketName=bucketName;
        this.secretAccessKey=secretAccessKey;
        this.accessKeyId=accessKeyId;
        this.region=region;
        this.s3Client = S3Client.builder().region(Region.of(region)).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey))).build();
    }


    public ResponseEntity<byte[]> serveFile(String deploymentId, String filePath) {

        String s3Key = "dist/" + deploymentId + filePath;

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        try {
            byte[] data = s3Client.getObjectAsBytes(getObjectRequest).asByteArray();

            String contentType = URLConnection.guessContentTypeFromName(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream"; // fallback for unrecognized extensions
            }

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .body(data);

        } catch (NoSuchKeyException e) {
            return ResponseEntity.notFound().build();
        }
    }



}
