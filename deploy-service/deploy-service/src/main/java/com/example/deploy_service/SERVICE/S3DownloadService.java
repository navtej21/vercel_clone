package com.example.deploy_service.SERVICE;


import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class S3DownloadService {

    /*
    Main-Aim : to download from S3 to my local disk
     */

    private final S3Client s3Client;
    private final String bucketName;


    public S3DownloadService(
            @org.springframework.beans.factory.annotation.Value("${aws.s3.bucket}") String bucketName,
            @org.springframework.beans.factory.annotation.Value("${aws.region}") String region,
            @org.springframework.beans.factory.annotation.Value("${aws.accessKeyId}") String accessKeyId,
            @Value("${aws.secretAccessKey}") String secretAccessKey) {

        this.bucketName = bucketName;

        // NOW accessKeyId and secretAccessKey have valid String values!
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build();
    }




    public void downloadDirectory(String s3Prefix,String localBaseDir) {

        // ensure s3Prefix ends with '/' for clean path resoln

        String normalizedPrefix = s3Prefix.endsWith("/") ? s3Prefix : s3Prefix + "/";


        //1 List Objects matching the prefix
        ListObjectsV2Request listrequest = ListObjectsV2Request.builder().bucket(bucketName).prefix(normalizedPrefix).build();


        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listrequest);


        for (S3Object s3Object : listResponse.contents()) {

            String key = s3Object.key();

            if (key.endsWith("/")) {
                continue;
            }


            // extract relative path
            String relativePath = key.substring(normalizedPrefix.length());

            // 2. Resolve target local file path
            Path destinationPath = Paths.get(localBaseDir, relativePath);

            try {
                // 3. Ensure local parent directories exist
                if (destinationPath.getParent() != null) {
                    Files.createDirectories(destinationPath.getParent());
                }

                // 4. Download file from S3 directly to local disk
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

                s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(destinationPath));
                System.out.println("Downloaded: " + key + " -> " + destinationPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
