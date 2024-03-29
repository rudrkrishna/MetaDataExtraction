package com.hashedin.MetaDataExtraction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import com.hashedin.MetaDataExtraction.dto.*;
import com.hashedin.MetaDataExtraction.model.Element;
import com.hashedin.MetaDataExtraction.repository.ElementsRepository;
import com.hashedin.MetaDataExtraction.utils.Constants;
import com.hashedin.MetaDataExtraction.utils.DownloadAndProcessingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static com.hashedin.MetaDataExtraction.utils.Constants.DELETED;

@Service
@Slf4j
public class MetaDataServiceImpl {

    private final BasicConfigProperties basicConfigProperties;
    private final RestTemplate restTemplate;
    private final ElementsRepository elementsRepository;
    private final DownloadAndProcessingUtil downloadAndProcessingUtil;


    @Autowired
    public MetaDataServiceImpl(BasicConfigProperties basicConfigProperties, RestTemplate restTemplate, ElementsRepository elementsRepository, DownloadAndProcessingUtil downloadAndProcessingUtil) {
        this.basicConfigProperties = basicConfigProperties;
        this.restTemplate = restTemplate;
        this.elementsRepository = elementsRepository;
        this.downloadAndProcessingUtil = downloadAndProcessingUtil;
    }

    public ResponseEntity<String> migrateMetaData(String workSpaceId) {
        boolean checkStatus = checkWorkspaceStatus(workSpaceId);
        if (!checkStatus) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No workspace found / Incorrect workspaceId : " + workSpaceId);
        }
        try {
            uploadMetaDataToWorkSpace(workSpaceId);
            return ResponseEntity.status(HttpStatus.OK).body("MetaData Translated Successfully for workspaceId : " + workSpaceId);
        } catch (Exception e) {
            log.error("Exception Caught while Iterating List of Element ID's, errorMessage : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Exception Caught while Iterating List of Element ID's present in the workspace : " + workSpaceId);
        }
    }

    private boolean checkWorkspaceStatus(String workSpaceId) {
        SonyCiWorkspaceDetailsResponse workspaceDetails = getWorkspaceDetails(workSpaceId);
        if (Objects.isNull(workspaceDetails)) {
            log.info("No workspace found / Incorrect workspaceId : {}", workSpaceId);
            return false;
        } else
            return true;
    }

    private SonyCiWorkspaceDetailsResponse getWorkspaceDetails(String workspaceId) {
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

    private void uploadMetaDataToWorkSpace(String workSpaceId) throws Exception {
        SonyCiListWorkspaceContentsResponse listWorkspaceContentsResponseDto = listWorkspaceContents(workSpaceId, 1, 0);
        if (!Objects.isNull(listWorkspaceContentsResponseDto)) {
            long totalContent = listWorkspaceContentsResponseDto.getCount();
            int limit = 25;
            int offset = 0;
            int pages = (int) Math.ceil(totalContent / (double) limit);
            for (int i = 0; i < pages; i++) {
                listWorkspaceContentsResponseDto = listWorkspaceContents(workSpaceId, limit, offset);
                if (!Objects.isNull(listWorkspaceContentsResponseDto)) {
                    Map<String, Items> assetDetailsMap = workspaceContents(listWorkspaceContentsResponseDto);
                    getElementsForAssets(assetDetailsMap, workSpaceId);
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

    private void getElementsForAssets(Map<String, Items> assetDetailsMap, String workSpaceId){
        int limit = 50;
        int offset = 0;
        Set<String> assetIds = assetDetailsMap.keySet();
        SonyCiBulkElementDetails sonyCiBulkElementDetails = getElementsForAssets(assetIds, offset, limit);
        if (!Objects.isNull(sonyCiBulkElementDetails)) {
            fetchAndUploadMetaData(sonyCiBulkElementDetails.getItems(), workSpaceId, assetDetailsMap);
            offset = sonyCiBulkElementDetails.getItems().size();
            int totalContent = sonyCiBulkElementDetails.getCount() - offset;
            int pages = (int) Math.ceil(totalContent / (double) limit);

            for (int i = 0; i < pages; i++) {
                sonyCiBulkElementDetails = getElementsForAssets(assetIds, offset, limit);
                if (!Objects.isNull(sonyCiBulkElementDetails)) {
                    log.info("Fetched {} elements detail for given {} assetIds ", sonyCiBulkElementDetails.getItems().size(), assetIds.size());
                    fetchAndUploadMetaData(sonyCiBulkElementDetails.getItems(), workSpaceId, assetDetailsMap);
                }
                offset += limit;
            }
        }
    }

    private void fetchAndUploadMetaData(List<ElementResponse> elementDetailsList, String workSpaceId, Map<String, Items> assetDetailsMap){
        if (!Objects.isNull(elementDetailsList)) {
            for (ElementResponse elementDetails : elementDetailsList) {
                if (downloadAndProcessingUtil.isXmlFile(elementDetails.getName()) && !DELETED.equalsIgnoreCase(elementDetails.getStatus())) {
                    fetchMetaDataFields(elementDetails, workSpaceId, assetDetailsMap);
                } else {
                    log.debug("Element is not a xml file, where elementId : {}", elementDetails.getId());
                }
            }
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

    private void fetchMetaDataFields(ElementResponse elementDetails, String workSpaceId, Map<String, Items> assetDetailsMap) {
        List<MetaDataFields> metaDataFieldList;
        String elementId = elementDetails.getId();
        boolean metaDataAddStatus;
        try {
            Optional<Element> dbElementDetails = elementsRepository.getElementDetails(workSpaceId, elementId, elementDetails.getAsset().getId());

            if (dbElementDetails.isEmpty()) {
                // translating elements which are present only in workspace
                metaDataFieldList = downloadAndProcessingUtil.fetchAndTranslateElement(elementDetails.getDownloadUrl(), elementId, workSpaceId);
                if(!Objects.isNull(metaDataFieldList)) {
                    Element newElement = new Element();
                    newElement.setAddedToCustomMetadata(false);
                    newElement.setAssetId(elementDetails.getAsset().getId());
                    newElement.setElementId(elementId);
                    newElement.setElementName(elementDetails.getName());
                    newElement.setWorkspaceId(workSpaceId);
                    Element savedElement = elementsRepository.save(newElement);

                    metaDataAddStatus = addMetaData(metaDataFieldList, elementDetails.getAsset().getId(), assetDetailsMap);
                    if (metaDataAddStatus) {
                        savedElement.setAddedToCustomMetadata(true);
                        elementsRepository.save(savedElement);
                    }
                }
            } else if (!dbElementDetails.get().getAddedToCustomMetadata()) {
                // translating elements which are migrated to workspace from s3
                metaDataFieldList = downloadAndProcessingUtil.fetchAndTranslateElement(elementDetails.getDownloadUrl(), elementId, workSpaceId);
                if(!Objects.isNull(metaDataFieldList)) {
                    metaDataAddStatus = addMetaData(metaDataFieldList, elementDetails.getAsset().getId(), assetDetailsMap);
                    if (metaDataAddStatus) {
                        dbElementDetails.get().setAddedToCustomMetadata(true);
                        elementsRepository.save(dbElementDetails.get());
                    }
                }
            } else {
                log.info("ElementId : {} present in workspaceId : {} is already uploaded", elementId, workSpaceId);
            }
        } catch (Exception e) {
            log.error("Something went wrong while fetching element details for elementId : {} present in workspaceId : {}, errorMessage : {}",
                    elementId, workSpaceId, e.getMessage());
        }
    }

    private boolean addMetaData(List<MetaDataFields> metaDataFieldList, String assetId, Map<String, Items> assetDetailsMap) {
        try {
            return uploadMetaData(metaDataFieldList, assetId);
        } catch (JsonProcessingException e) {
            log.error("Something went wrong while parsing json, errorMessage : {}", e.getMessage());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.error("MetaData Already Exist for assetId : {}, errorMessage : {}", assetId, e.getMessage());
                return mergeExistingMetaDataAndRetryUpload(metaDataFieldList, assetId, assetDetailsMap.get(assetId).getMetadata());
            }
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("MetaData not updated as payLoad format mismatch for assetId : {}, errorMessage : {}", assetId, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Something went wrong while adding custom metadata fields to assetId : {}, errorMessage : {}", assetId, e.getMessage());
        }
        return false;
    }

    private boolean mergeExistingMetaDataAndRetryUpload(List<MetaDataFields> metaDataFieldList, String assetId, List<MetaDataFields> existingAssetMetaData) {

        Map<String, MetaDataFields> combineMetaDataMap = metaDataFieldList.stream().collect(Collectors.toMap(metaData -> metaData.getName().toLowerCase(), metaData -> metaData));
        try {
            for(MetaDataFields metaData: existingAssetMetaData){
                if(combineMetaDataMap.containsKey(metaData.getName().toLowerCase())){
                    deleteMetaData(assetId, metaData.getName());
                }
            }

            List<MetaDataFields> metaDataFields = new ArrayList<>(combineMetaDataMap.values());
            return uploadMetaData(metaDataFields, assetId);

        } catch (JsonProcessingException e) {
            log.error("Something went wrong while parsing json, errorMessage : {}", e.getMessage());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.error("MetaData Already Exist for assetId : {}, errorMessage : {}", assetId, e.getMessage());
            }
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("MetaData not updated as payLoad format mismatch for assetId : {}, errorMessage : {}", assetId, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Something went wrong while adding custom metadata fields to assetId : {}, errorMessage : {}", assetId, e.getMessage());
        }
        return false;
    }

    private boolean uploadMetaData(List<MetaDataFields> metaDataFieldList, String assetId) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFormat = mapper.writeValueAsString(new MetaDataFormat(metaDataFieldList));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(jsonFormat, httpHeaders);
        restTemplate.exchange(basicConfigProperties.getAddMetaDataApi() +
                        assetId + "/metadata", HttpMethod.POST, entity,
                new ParameterizedTypeReference<>() {
                });
        log.info("MetaData updated in corresponding asset custom metaData fields where assetId : {}", assetId);
        return true;
    }

    private void deleteMetaData(String assetId, String fieldName) {
        log.info("Deleting MetaData fieldName : {} for the corresponding assetId : {}", fieldName, assetId);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
        restTemplate.exchange(basicConfigProperties.getDeleteMetaData() +
                        assetId + "/metadata/" + fieldName , HttpMethod.DELETE, entity,
                new ParameterizedTypeReference<>() {
                });
        log.info("MetaData fieldName : {} deleted for the corresponding assetId : {}", fieldName, assetId);
    }

    public ResponseEntity<String> deleteWorkSpaceMetaData(String workSpaceId){
        boolean checkStatus = checkWorkspaceStatus(workSpaceId);
        if (!checkStatus) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No workspace found / Incorrect workspaceId : " + workSpaceId);
        }
        try {
            SonyCiListWorkspaceContentsResponse listWorkspaceContentsResponseDto = listWorkspaceContents(workSpaceId, 1, 0);
            if (!Objects.isNull(listWorkspaceContentsResponseDto)) {
                long totalContent = listWorkspaceContentsResponseDto.getCount();
                int limit = 25;
                int offset = 0;
                int pages = (int) Math.ceil(totalContent / (double) limit);
                for (int i = 0; i < pages; i++) {
                    listWorkspaceContentsResponseDto = listWorkspaceContents(workSpaceId, limit, offset);
                    if (!Objects.isNull(listWorkspaceContentsResponseDto)) {
                        Map<String, Items> assetDetailsMap = workspaceContents(listWorkspaceContentsResponseDto);
                            for (String assetId : assetDetailsMap.keySet()) {
                                if (!Objects.isNull(assetDetailsMap.get(assetId).getMetadata())) {
                                    for (MetaDataFields metaData : assetDetailsMap.get(assetId).getMetadata()) {
                                        try {
                                            deleteMetaData(assetId, metaData.getName());
                                        } catch (Exception e) {
                                            log.error("Something went wrong in deleting metadata for assetId : {}, errorMessage : {}",
                                                    assetId, e.getMessage());
                                        }
                                    }
                                }
                            }
                        offset += limit;
                    }
                }
                log.info("Deleted all metaData from workspace : {}", workSpaceId);
                elementsRepository.deleteByWorkspaceId(workSpaceId);
                return ResponseEntity.status(HttpStatus.OK).body("MetaData deleted Successfully for workspaceId : " + workSpaceId);
            } else {
                log.error("No details received from the list workspace api, where workspaceId : {}", workSpaceId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No assets are present for given workspaceId : " + workSpaceId);
            }
        } catch (Exception e) {
            log.error("Exception Caught while Iterating List of Element ID's, errorMessage : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Exception Caught while Iterating List of Element ID's present in the workspace : " + workSpaceId);
        }
    }

}