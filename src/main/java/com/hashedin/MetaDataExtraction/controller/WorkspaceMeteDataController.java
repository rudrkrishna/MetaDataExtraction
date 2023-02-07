package com.hashedin.MetaDataExtraction.controller;


import com.hashedin.MetaDataExtraction.dto.ElementRequest;
import com.hashedin.MetaDataExtraction.service.MetaDataServiceImpl;
import com.hashedin.MetaDataExtraction.service.WorkSpaceMetaDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@RestController
@EnableWebMvc
public class WorkspaceMeteDataController {

    private final MetaDataServiceImpl metaDataService;
    private final WorkSpaceMetaDataServiceImpl workSpaceMetaDataService;

    @Autowired
    public WorkspaceMeteDataController(MetaDataServiceImpl metaDataService,
                                       WorkSpaceMetaDataServiceImpl workSpaceMetaDataService) {
        this.metaDataService = metaDataService;
        this.workSpaceMetaDataService = workSpaceMetaDataService;
    }

    @GetMapping("/deleteMetaData")
    public String deleteMetaData(){
        workSpaceMetaDataService.deleteData();
        return "MetaData Deleted Successfully";
    }

    @PostMapping("/getMetaData")
    public ResponseEntity<?> getMetaData(@RequestBody ElementRequest elementId){
        return metaDataService.getMetaData(elementId.getElementId());
    }
}
