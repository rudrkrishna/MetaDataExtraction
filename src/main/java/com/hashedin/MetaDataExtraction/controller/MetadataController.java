package com.hashedin.MetaDataExtraction.controller;

import com.hashedin.MetaDataExtraction.service.MetaDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class MetadataController {

    private final MetaDataServiceImpl metaDataService;
    @Autowired
    public MetadataController(MetaDataServiceImpl metaDataService) {
        this.metaDataService = metaDataService;
    }

    @GetMapping("/workSpaceMetaData")
    public ResponseEntity<String> migrateWorkspaceMetaData(@RequestParam("workSpaceId") String workSpaceId){
        ResponseEntity<String> workspaceStatus=metaDataService.migrateMetaData(workSpaceId);
        if(workspaceStatus.getStatusCode().is2xxSuccessful()) {
            log.info(workspaceStatus.getBody());
            return ResponseEntity.status(HttpStatus.OK).body(workspaceStatus.getBody());
        } else {
            log.error(workspaceStatus.getBody());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(workspaceStatus.getBody());
        }
    }

    @DeleteMapping("/deleteWorkSpaceMetaData")
    public ResponseEntity<String> deleteWorkSpaceMetaData(@RequestParam("workSpaceId") String workSpaceId){
        return metaDataService.deleteWorkSpaceMetaData(workSpaceId);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> healthCheck(){
        return ResponseEntity.status(HttpStatus.OK).body("pong");
    }

}