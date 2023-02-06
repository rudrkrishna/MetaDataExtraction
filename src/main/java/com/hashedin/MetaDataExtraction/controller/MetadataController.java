package com.hashedin.MetaDataExtraction.controller;

import com.hashedin.MetaDataExtraction.dto.ElementRequest;
import com.hashedin.MetaDataExtraction.dto.ElementResponse;
import com.hashedin.MetaDataExtraction.dto.MetaDataFormat;
import com.hashedin.MetaDataExtraction.service.MetaDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class MetadataController {

    private final MetaDataServiceImpl metaDataService;
    @Autowired
    public MetadataController(MetaDataServiceImpl metaDataService) {
        this.metaDataService = metaDataService;
    }

    @PostMapping("/getMetaData")
    public ResponseEntity<?> getMetaData(@RequestBody ElementRequest elementId){
        return metaDataService.getMetaData(elementId.getElementId());
    }

    @GetMapping("/cronjob")
    @Scheduled(cron="0 */2 * * * *")
    public void getElementId(){
        metaDataService.dbElements();
    }

}