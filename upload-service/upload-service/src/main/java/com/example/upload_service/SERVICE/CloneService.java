package com.example.upload_service.SERVICE;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

@Service
public class CloneService {


    // generate a random uuid for which matches the netlify/vercel hashes (somewhat!!)
    public String generateUID(){
        UUID uuid=UUID.randomUUID();
        return uuid.toString();
    }

    /*

    writing a function that clones my repo from the remote github to the disk.
     */

    public void cloneRepo(String repoUrl,String id) throws GitAPIException, IOException{


        // create a directory object
        File destinationDir=new File("./output/"+id);

        // if the destinationfolder is not there then we make it
        if(!destinationDir.exists()){
            destinationDir.mkdir();
        }


        try{
            Git git=Git.cloneRepository().setURI(repoUrl).setDirectory(destinationDir).call();
            System.out.println("Successfully Cloned"+repoUrl);
            git.close();
        }
        catch (Exception e){

        }
    }
}
