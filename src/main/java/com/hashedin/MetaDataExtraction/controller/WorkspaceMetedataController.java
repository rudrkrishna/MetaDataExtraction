package com.hashedin.MetaDataExtraction.controller;


import com.hashedin.MetaDataExtraction.dto.ElementResponse;
import com.hashedin.MetaDataExtraction.service.MetaDataServiceImpl;
import com.hashedin.MetaDataExtraction.service.WorkSpaceMetaDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspace")
public class WorkspaceMetedataController {

    private final MetaDataServiceImpl metaDataService;
    private final WorkSpaceMetaDataServiceImpl workSpaceMetaDataService;

    @Autowired
    public WorkspaceMetedataController(MetaDataServiceImpl metaDataService,
                                       WorkSpaceMetaDataServiceImpl workSpaceMetaDataService) {
        this.metaDataService = metaDataService;
        this.workSpaceMetaDataService = workSpaceMetaDataService;
    }

    @GetMapping("/workSpaceMetaData")
    public String migrateWorkspaceMetaData(){
       List<String> elementIds= workSpaceMetaDataService.listWorkspaceContents();
        for(String s:elementIds){
            ResponseEntity<ElementResponse> response= metaDataService.getDownloadableUrl(s);
            if(metaDataService.isXmlFile(response)) {
                metaDataService.addMetaData(metaDataService.fetchMetaDataFields(response));
            }
        }
        workSpaceMetaDataService.flushDS();
        return "MetaData Translated Successfully";
    }
}
