package com.hashedin.MetaDataExtraction.controller;

import com.hashedin.MetaDataExtraction.service.WorkSpaceMetaDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@RestController
@EnableWebMvc
public class MetadataController {

    private final WorkSpaceMetaDataServiceImpl workSpaceMetaDataService;
    @Autowired
    public MetadataController(WorkSpaceMetaDataServiceImpl workSpaceMetaDataService) {
        this.workSpaceMetaDataService = workSpaceMetaDataService;
    }

    @GetMapping("/workSpaceMetaData")
    public ResponseEntity<String> migrateWorkspaceMetaData(@RequestParam String workSpaceId){
        ResponseEntity<String> workspaceStatus=workSpaceMetaDataService.migrateMetaData(workSpaceId);
        if(workspaceStatus.getStatusCode().is5xxServerError()){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(workspaceStatus.getBody());
        }
        return ResponseEntity.status(HttpStatus.OK).body(workspaceStatus.getBody());
    }

}