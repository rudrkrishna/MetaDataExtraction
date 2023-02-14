package com.hashedin.MetaDataExtraction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import com.hashedin.MetaDataExtraction.dto.*;
import com.hashedin.MetaDataExtraction.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static com.hashedin.MetaDataExtraction.utils.Constants.DELETED;

@Service
@Slf4j
public class WorkSpaceMetaDataServiceImpl {
    private final BasicConfigProperties basicConfigProperties;
    private final RestTemplate restTemplate;
    private final MetaDataServiceImpl metaDataService;

    @Autowired
    public WorkSpaceMetaDataServiceImpl(BasicConfigProperties basicConfigProperties,
                                        RestTemplate restTemplate,
                                        MetaDataServiceImpl metaDataService) {
        this.basicConfigProperties = basicConfigProperties;
        this.restTemplate = restTemplate;
        this.metaDataService = metaDataService;
    }

    public ResponseEntity<String> migrateMetaData(String workSpaceId) {
        boolean checkStatus = checkWorkspaceStatus(workSpaceId);
        if (!checkStatus) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No workspace found / Incorrect workspaceId : " + workSpaceId);
        }
        try {
            listWorkspaceContents(workSpaceId);
            return ResponseEntity.status(HttpStatus.OK).body("MetaData Translated Successfully for workspaceId : " + workSpaceId);
        } catch (Exception e) {
            log.error("Exception Caught while Iterating List of Element ID's, errorMessage : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Exception Caught while Iterating List of Element ID's present in the workspace : " + workSpaceId);
        }
    }

    public void listWorkspaceContents(String workSpaceId) throws Exception {
        SonyCiListWorkspaceContentsResponse listWorkspaceContentsResponseDto = listWorkspaceContents(workSpaceId, 1, 0);
        List<ElementResponse> elementDetailsList;
        if (!Objects.isNull(listWorkspaceContentsResponseDto)) {
            long totalContent = listWorkspaceContentsResponseDto.getCount();
            int limit = 25;
            int offset = 0;
            int pages = (int) Math.ceil(totalContent / (double) limit);
            for (int i = 0; i < pages; i++) {
                listWorkspaceContentsResponseDto = listWorkspaceContents(workSpaceId, limit, offset);
                if (!Objects.isNull(listWorkspaceContentsResponseDto)) {
                    Map<String, Items> assetDetailsMap = workspaceContents(listWorkspaceContentsResponseDto);
                    elementDetailsList = getElementsForAssets(assetDetailsMap.keySet());
                    if (!Objects.isNull(elementDetailsList)) {
                        for (ElementResponse elementDetails : elementDetailsList) {
                            if (metaDataService.isXmlFile(elementDetails.getName()) && !DELETED.equalsIgnoreCase(elementDetails.getStatus())) {
                                metaDataService.fetchMetaDataFields(elementDetails, workSpaceId, assetDetailsMap);
                            } else {
                                log.debug("Element is not a xml file, where elementId : {}", elementDetails.getId());
                            }
                        }
                    }
                    offset += limit;
                }
            }
        } else {
            log.error("No details received from the list workspace api, where workspaceId : {}", workSpaceId);
            throw new Exception("No assets are present for given workspaceId : " + workSpaceId);
        }
    }

    private SonyCiListWorkspaceContentsResponse listWorkspaceContents(String workSpaceId, int limit, int offset) {
        try {
            log.info("Fetching workspace contents for workspaceId : {} where offset : {} and limit : {}", workSpaceId, offset, limit);
            ResponseEntity<SonyCiListWorkspaceContentsResponse> responseEntity;
            String param = workSpaceId +
                    "/contents?kind=Asset&" +
                    "limit=" + (limit) + "&" +
                    "offset=" + (offset) + "&" +
                    "orderBy=createdOn&" +
                    "orderDirection=asc&" +
                    "fields=parentFolder,folder,metadata,status";
            String url = basicConfigProperties.getListWorkspaceContentsURL() + param;
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<>() {
                    });
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Http status code exception raised while fetching workspace contents for workspaceId : {} where limit : {} and offset : {}, errorMessage : {}",
                    workSpaceId, limit, offset, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Something went wrong while fetching workspace contents for workspaceId : {}  where limit : {} and offset : {}, errorMessage : {}",
                    workSpaceId, limit, offset, e.getMessage());
            return null;
        }
    }

    private Map<String, Items> workspaceContents(SonyCiListWorkspaceContentsResponse listWorkspaceContentsResponseDto) {
        Map<String, Items> assetDetailsMap = new HashMap<>();
        try {
            List<Items> itemsList = listWorkspaceContentsResponseDto.getItems();
            for (Items item : itemsList) {
                if (item.getKind().equalsIgnoreCase(Constants.ASSET)) {
                    assetDetailsMap.put(item.getId(), item);
                }
            }
            log.info("All {} Items fetched from Workspace", assetDetailsMap.keySet().size());
            return assetDetailsMap;
        } catch (Exception e) {
            log.error("Exception occurred while filtering the workspace content for assets / NullPointerException occurred, errorMessage : {}", e.getMessage());
            return assetDetailsMap;
        }
    }

    private List<ElementResponse> getElementsForAssets(Set<String> assetIds){
        List<ElementResponse> elementResponseList;
        int limit = 50;
        int offset = 0;
        SonyCiBulkElementDetails sonyCiBulkElementDetails = getElementsForAssets(assetIds, offset, limit);
        if (!Objects.isNull(sonyCiBulkElementDetails)) {
            elementResponseList = new ArrayList<>(sonyCiBulkElementDetails.getItems());
            offset = sonyCiBulkElementDetails.getItems().size();
            int totalContent = sonyCiBulkElementDetails.getCount() - offset;
            int pages = (int) Math.ceil(totalContent / (double) limit);

            for (int i = 0; i < pages; i++) {
                sonyCiBulkElementDetails = getElementsForAssets(assetIds, offset, limit);
                if (!Objects.isNull(sonyCiBulkElementDetails)) {
                    elementResponseList.addAll(sonyCiBulkElementDetails.getItems());
                }
                offset += limit;
            }
            log.info("Fetched {} elements detail for given {} assetIds ", elementResponseList.size(), assetIds.size());
            return elementResponseList;
        } else {
            return null;
        }
    }


    private SonyCiBulkElementDetails getElementsForAssets(Set<String> assetIds, int offset, int limit) {
        log.info("Fetching list of element details for given list of {} assetIds, offset : {}, limit : {}", assetIds.size(), offset, limit);
        ResponseEntity<SonyCiBulkElementDetails> response;
        String url = basicConfigProperties.getBulkAssetOrElementDetails();

        BulkDetailsPayload bulkDetailsPayload = new BulkDetailsPayload();
        bulkDetailsPayload.setAssetIds(assetIds);
        bulkDetailsPayload.setOffset(offset);
        bulkDetailsPayload.setLimit(limit);

        ObjectMapper mapper = new ObjectMapper();
        String jsonFormat;
        try {
            jsonFormat = mapper.writeValueAsString(bulkDetailsPayload);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());

            HttpEntity<String> entity = new HttpEntity<>(jsonFormat, httpHeaders);
            response = restTemplate.exchange(url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<>() {
                    });
            log.info("Fetched list of element details for given list of {} assetIds", assetIds.size());
            return Objects.requireNonNull(response.getBody());
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while processing json : {}", e.getMessage());
        } catch (HttpStatusCodeException e) {
            log.error("Exception caught while fetching bulk elements details, errorMessage : {}", e.getMessage());
        } catch (Exception e){
            log.error("Something went wrong while fetching bulk element details, errorMessage : {}", e.getMessage());
        }
        return null;
    }

    public boolean checkWorkspaceStatus(String workSpaceId) {
        SonyCiWorkspaceDetailsResponse workspaceDetails = getWorkspaceDetails(workSpaceId);
        if (Objects.isNull(workspaceDetails)) {
            log.info("No workspace found / Incorrect workspaceId : {}", workSpaceId);
            return false;
        } else
            return true;
    }

    public SonyCiWorkspaceDetailsResponse getWorkspaceDetails(String workspaceId) {
        ResponseEntity<SonyCiWorkspaceDetailsResponse> responseEntity;
        final String URL = basicConfigProperties.getGetWorkspaceDetailsURL() + workspaceId;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
        try {
            responseEntity = restTemplate.exchange(URL, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<>() {
                    });
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("Something went wrong while fetching details of workspace with id : {}, errorMessage : {}", workspaceId, e.getMessage());
            return null;
        }
    }

}
