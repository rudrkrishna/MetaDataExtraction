package com.hashedin.MetaDataExtraction.controller;

import com.hashedin.MetaDataExtraction.service.MetaDataServiceImpl;
import com.hashedin.MetaDataExtraction.service.WorkSpaceMetaDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

@RestController
public class MetadataController {

    private final MetaDataServiceImpl metaDataService;
    private final WorkSpaceMetaDataServiceImpl workSpaceMetaDataService;
    @Autowired
    public MetadataController(MetaDataServiceImpl metaDataService, WorkSpaceMetaDataServiceImpl workSpaceMetaDataService) {
        this.metaDataService = metaDataService;
        this.workSpaceMetaDataService = workSpaceMetaDataService;
    }

    @GetMapping("/workSpaceMetaData")
    public String migrateWorkspaceMetaData(){
        workSpaceMetaDataService.migrateMetaData();
        return "MetaData Translated Successfully";
    }

    @GetMapping("/s3MigratedMetaDataTranslation")
    @Scheduled(cron="0 */2 * * * *")
    public void getElementId(){
        metaDataService.dbElements();
    }

}