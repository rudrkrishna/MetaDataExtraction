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
    public ResponseEntity<MetaDataFormat> getMetaData(@RequestBody ElementRequest elementId){
        ResponseEntity<ElementResponse> response= metaDataService.getDownloadableUrl(elementId.getElementId());
        if(metaDataService.isXmlFile(response)) {
            return metaDataService.addMetaData(metaDataService.fetchMetaDataFields(response));
        }else{
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    @GetMapping("/cronjob")
    @Scheduled(cron="*/1 * * * *")
    public void getElementId(){
        List<String> elementIds=metaDataService.getElementsId();
        for(String s:elementIds){
            ResponseEntity<ElementResponse> response= metaDataService.getDownloadableUrl(s);
            if(metaDataService.isXmlFile(response)) {
                metaDataService.addMetaData(metaDataService.fetchMetaDataFields(response));
                metaDataService.changeStatusInDb(s);
            }
        }

    }

}