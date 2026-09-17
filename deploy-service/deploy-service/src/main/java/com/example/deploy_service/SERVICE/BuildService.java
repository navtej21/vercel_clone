package com.example.deploy_service.SERVICE;


import lombok.AllArgsConstructor;
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
import java.time.chrono.ThaiBuddhistEra;
import java.util.stream.Stream;


/*

so since the architecutre we are focussing is only on client-side rather than server-side we
so i am not going to do any sandboxing for this section so everything we build across
is pushed into a s3 bucket


 */
@Service
public class BuildService {

    private final String bucketName;

    private final S3Client s3Client;


    public BuildService( @Value("${aws.s3.bucket}") String bucketName,
                         @Value("${aws.region}") String region,
                         @Value("${aws.accessKeyId}") String accessKeyId,
                         @Value("${aws.secretAccessKey}") String secretAccessKey
                         ){

        this.bucketName=bucketName;
        this.s3Client=S3Client.builder().region(Region.of(region)).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId,secretAccessKey))).build();

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
    public void uploadDirectory(String id) throws IOException{

        Path baseDir=Path.of("./output/"+id+"/dist");

        if(!Files.exists(baseDir)){
            System.out.println("No Directory Exists");
            return;
        }
        Stream<Path> stream=Files.walk(baseDir);

        try{
            stream.filter(Files::isRegularFile).forEach(filePath->{
                // get the relative pathf for each of the file in the artificat
                Path relativePath=baseDir.relativize(filePath);
                // relativize the path so that it can be stored in the amazon S3 bucket instance
                String relativeKey=relativePath.toString().replace("\\","/");
                // Create a Put Object Request
                String s3Key="dist/"+id+"/"+relativeKey;
                uploadSingleFile(s3Key,filePath.toFile());
            });
        }
        catch(Exception e){
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }

    }


}
