package com.example.deploy_service.SERVICE;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;


/*

so since the architecutre we are focussing is only on client-side rather than server-side we
so i am not going to do any sandboxing for this section so everything we build across
is pushed into a s3 bucket


 */
@Service
public class BuildService {

    private final String bucketName;
    private final String region;
    private final String accessKeyId;
    private final String secretAccessKey;

    private final S3Client s3Client;


    public BuildService( @Value("${aws.s3.bucket}") String bucketName,
                         @Value("${aws.region}") String region,
                         @Value("${aws.accessKeyId}") String accessKeyId,
                         @Value("${aws.secretAccessKey}") String secretAccessKey,
                         S3Client s3Client){

        this.bucketName=bucketName;
        this.region=region;
        this.accessKeyId=accessKeyId;
        this.secretAccessKey=secretAccessKey;
        this.s3Client=s3Client;

    }

    public boolean buildProject(String id) throws IOException,InterruptedException{

        File projectDir=new File("./output/"+id);
        ProcessBuilder processBuilder=new ProcessBuilder("cmd.exe","/c","npm install && npm run build");
        processBuilder.directory(projectDir);
        processBuilder.inheritIO();
        Process process=processBuilder.start();
        int exitCode=process.waitFor();
        return exitCode==0;

    }


    /*
     same as upload service we will write a function that involves uploading a single file
     into a s3 bucket
     */
    public void uploadSingleFile(String s3Key,File file){
        // check if the file exists
        if(!file.exists()){
            System.out.println("File Doesn't exists");
            return;
        }
        // create a putrequest Object
        PutObjectRequest putObjectRequest=PutObjectRequest.builder().bucket(bucketName).key(s3Key).build();
        // push the file into the s3bucket
        s3Client.putObject(putObjectRequest,file.toPath());
    }


    /*
    now we are going to push the entire built-client side artificat to the s3bucket of another url
     */


}
