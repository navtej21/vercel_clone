package com.example.upload_service.CONTROLLER;


import com.example.upload_service.MODEL.Repo;
import com.example.upload_service.SERVICE.CloneService;
import com.example.upload_service.SERVICE.S3Service;
import com.example.upload_service.SERVICE.SqsService;
import com.example.upload_service.SERVICE.StatusService;
import lombok.AllArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;



@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class Controller {



    // call the CloneService Object
    private final CloneService cloneService;
    private final S3Service s3Service;
    private final SqsService sqsService;
    private final StatusService statusService;
    /*
     now we will combine everything into this controller. that is bring the github repo to the disk from the disk get into the  s3-Bucket
     */
    @PostMapping("/deploy")
    public ResponseEntity<?> deployRepo(@RequestBody Repo repo) throws IOException {

        String repoUrl=repo.getRepoUrl();

        // check if the repourl is empty or null
        if(repoUrl==null ||  repoUrl.isEmpty()){
            return ResponseEntity.badRequest().body(Map.of("error","RepoUrl cannot be empty"));
        }


        // generate a random UUID
        String deploymentId=cloneService.generateUID();
        try{

            // this get the repo from the cloud to the disk
            cloneService.cloneRepo(repoUrl,deploymentId);
            // now we will upload the directory from the disk to the S3 bucket
            s3Service.uploadDirectory(deploymentId);

            // push the deployment id into the messaging queue
            String messageId= sqsService.sendMessage(deploymentId);

            // we will implement the statusservice function here
            statusService.updateStatus(deploymentId,"updated");

            return ResponseEntity.ok().body(Map.of(
                    "id", deploymentId,
                    "status", "uploaded",
                    "messageId",messageId,
                    "message", "Repository cloned and uploaded to S3 successfully"
            ));
        }
        catch(Exception e){
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error", "Deployment failed: " + e.getMessage()
                    )
            );
        }
    }


    @GetMapping("/status/{id}")
    public ResponseEntity<String> getStatus(@PathVariable String id){
        return ResponseEntity.ok().body(statusService.getStatusUpdate(id));
    }
}
