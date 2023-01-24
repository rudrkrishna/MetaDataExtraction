package com.hashedin.MetaDataExtraction.controller;

import com.hashedin.MetaDataExtraction.dto.ElementRequest;
import com.hashedin.MetaDataExtraction.dto.ElementResponse;
import com.hashedin.MetaDataExtraction.dto.MetaData;
import com.hashedin.MetaDataExtraction.service.MetaDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class MetadataController {

    private final MetaDataServiceImpl metaDataService;
    @Autowired
    public MetadataController(MetaDataServiceImpl metaDataService) {
        this.metaDataService = metaDataService;
    }

    @PostMapping("/getMetaData")
    public ResponseEntity<MetaData> getMetaData(@RequestBody ElementRequest elementId){
        ResponseEntity<ElementResponse> response= metaDataService.getDownloadableUrl(elementId.getElementId());
        if(metaDataService.isXmlFile(response.getBody().getName())) {
            return metaDataService.addMetaData(metaDataService.fetchMetaDataFields(response));
        }else{
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }
}
