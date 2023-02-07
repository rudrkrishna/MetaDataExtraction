package com.hashedin.MetaDataExtraction.controller;

import com.hashedin.MetaDataExtraction.service.MetaDataServiceImpl;
import com.hashedin.MetaDataExtraction.service.WorkSpaceMetaDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@RestController
@EnableWebMvc
public class MetadataController {

    private final MetaDataServiceImpl metaDataService;
    private final WorkSpaceMetaDataServiceImpl workSpaceMetaDataService;
    @Autowired
    public MetadataController(MetaDataServiceImpl metaDataService, WorkSpaceMetaDataServiceImpl workSpaceMetaDataService) {
        this.metaDataService = metaDataService;
        this.workSpaceMetaDataService = workSpaceMetaDataService;
    }

    @GetMapping("/workSpaceMetaData")
    public ResponseEntity<String> migrateWorkspaceMetaData(){
        workSpaceMetaDataService.migrateMetaData();
        return ResponseEntity.status(HttpStatus.OK).body("MetaData Translated Successfully");
    }

    @PostMapping("/s3MigratedMetaDataTranslation")
    @Scheduled(cron="0 */2 * * * *")
    public void getElementId(){
        metaDataService.dbElements();
    }

}