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
        ResponseEntity<ElementResponse> response= metaDataService.getDownloadableUrl(elementId.getElementId());
        if(metaDataService.isXmlFile(response)) {
            return new ResponseEntity<>(metaDataService.addMetaData(metaDataService.fetchMetaDataFields(response)),HttpStatus.OK);
        }else{
            return new ResponseEntity<>("Not an XML",HttpStatus.OK);
        }
    }

    @GetMapping("/cronjob")
    @Scheduled(cron="*/1 * * * *")
    public void getElementId(){

        metaDataService.dbElements();

    }

}