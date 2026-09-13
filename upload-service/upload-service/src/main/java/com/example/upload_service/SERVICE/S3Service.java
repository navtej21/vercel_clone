package com.example.upload_service.SERVICE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;


// now we are combining both the (SINGLE-FILE upload into aws s3 bucket and with the given directory)
@Service
public class S3Service {

    private final String bucketName;
    private final S3Client s3Client;

    // 1. Correct syntax: Use ${...} and non-static fields
    public S3Service(
            @Value("${aws.s3.bucket}") String bucketName,
            @Value("${aws.region}") String region,
            @Value("${aws.accessKeyId}") String accessKeyId,
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


    // we will define a function that performs a single file upload into a s3bucket
    public void uploadSingleFile(String s3Key,File localFile){

        // if the local file is not there
        if (!localFile.exists()){
            System.out.println("There is no file available");
            return;
        }

        // create a put object request
        PutObjectRequest putObjectRequest= PutObjectRequest.builder().bucket(bucketName).key(s3Key).build();

        // now we will push our localfile into the s3Bucket
        s3Client.putObject(putObjectRequest,localFile.toPath());
    }


    // now we will write a function that recursivley runs accross /{uid} in the directory and uploads those into the S3 bucket
    public  void uploadDirectory(String id) throws IOException{
        Path baseDir= Paths.get("./output/"+id);

        // check if the directory exists
        if(!Files.exists(baseDir)){
            System.out.println("No Directory Exists");
            return;
        }

        // we will recursively run through the files
        Stream<Path> stream=Files.walk(baseDir);
        try{
            stream.filter(Files::isRegularFile).forEach(filePath->{

                // compute the relative path
                Path relativePath=baseDir.relativize(filePath);


                // standardize slasesh for S3-bucket
                String relativeKey=relativePath.toString().replace("\\","/");

                // get the output s3Key Path
                String s3Key="output/"+id+"/"+relativeKey;


                // upload it with our base uploading function
                uploadSingleFile(s3Key,filePath.toFile());
            });
        }
        catch(Exception e){

        }
    }

}