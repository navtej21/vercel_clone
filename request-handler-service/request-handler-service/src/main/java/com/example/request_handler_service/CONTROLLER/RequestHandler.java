package com.example.request_handler_service.CONTROLLER;

import com.example.request_handler_service.SERVICE.RequestService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@RestController
@AllArgsConstructor
public class RequestHandler {


    private RequestService requestService;

    @GetMapping("/**")
    public ResponseEntity<?> getHostName(
            @RequestHeader("host") String host,
            HttpServletRequest request) {

        String deploymentId = extractDeploymentId(host);
        if (deploymentId == null) {
            return ResponseEntity.badRequest().body("DeploymentId Cannot Be Null");
        }

        String filePath = request.getRequestURI();
        if (filePath.equals("/")) {
            filePath = "/index.html";
        }

        return requestService.serveFile(deploymentId, filePath);
    }

    // spliting extracting the deployment id to showcase as the url
    private String extractDeploymentId(String host){

        if(!host.contains(".")){
            return null;
        }

        return host.split("\\.")[0];
    }
}
