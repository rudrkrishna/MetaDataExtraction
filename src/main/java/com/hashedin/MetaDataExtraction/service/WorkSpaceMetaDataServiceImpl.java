package com.hashedin.MetaDataExtraction.service;

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

    public void listWorkspaceContents(String workSpaceId) {
        SonyCiListWorkspaceContentsResponse listWorkspaceContentsResponseDto = listWorkspaceContents(workSpaceId, 1, 0);
        List<String> elementIds;
        if (!Objects.isNull(listWorkspaceContentsResponseDto)) {
            long totalContent = listWorkspaceContentsResponseDto.getCount();
            int limit = 100;
            int offset = 0;
            int pages = (int) Math.ceil(totalContent / (double) limit);
            for (int i = 1; i <= pages; i++) {
                List<String> assetIds = new ArrayList<>();
                listWorkspaceContentsResponseDto = listWorkspaceContents(workSpaceId, limit, offset);
                if (!Objects.isNull(listWorkspaceContentsResponseDto)) {
                    assetIds = workspaceContents(listWorkspaceContentsResponseDto);
                    offset += limit;
                }
                elementIds = getElementsForAssets(assetIds);
                for (String elementId : elementIds) {
                    ElementResponse response = metaDataService.getDownloadableUrl(elementId);
                    if (!Objects.isNull(response) && !DELETED.equalsIgnoreCase(response.getStatus())) {
                        if (metaDataService.isXmlFile(response.getName())) {
                            metaDataService.fetchMetaDataFields(response, workSpaceId);
                        }
                    } else {
                        log.error("Invalid ElementID / Element is Deleted, where elementId : {}", elementId);
                    }
                }
            }
        } else {
            log.error("No details received from the list workspace api, where workspaceId : {}", workSpaceId);
        }
    }

    private SonyCiListWorkspaceContentsResponse listWorkspaceContents(String workSpaceId, int limit, int offset) {
        log.info("Fetching workspace contents for workspaceId : {}", workSpaceId);
        ResponseEntity<SonyCiListWorkspaceContentsResponse> responseEntity;
        String param = workSpaceId +
                "/contents?kind=Asset&" +
                "limit=" + (limit) + "&" +
                "offset=" + (offset) + "&" +
                "orderBy=createdOn&" +
                "orderDirection=asc&" +
                "fields=parentFolder,folder";
        String url = basicConfigProperties.getListWorkspaceContentsURL() + param;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
        try {
            responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<>() {
                    });
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Http status code exception raised while fetching workspace contents, errorMessage : {}", e.getMessage());
            return null;
        } catch (Exception e){
            log.error("Something went wrong while fetching workspace contents, errorMessage : {}", e.getMessage());
            return null;
        }
    }

    private List<String> workspaceContents(SonyCiListWorkspaceContentsResponse listWorkspaceContentsResponseDto) {
        List<String> assetIds = new ArrayList<>();
        try {
            List<Items> itemsList = listWorkspaceContentsResponseDto.getItems();
            log.info("All Items fetched from Workspace");
            for (Items item : itemsList) {
                if (item.getKind().equalsIgnoreCase(Constants.ASSET)) {
                    assetIds.add(item.getId());
                }
            }
            return assetIds;
        } catch (Exception e) {
            log.error("Exception occurred while filtering the workspace content for assets / NullPointerException occurred, errorMessage : {}", e.getMessage());
            return assetIds;
        }
    }


    private List<String> getElementsForAssets(List<String> assetIds) {
        List<String> elementIds = new ArrayList<>();
        try {
            if (assetIds.size() > 0) {
                log.info("Element Id's are being fetched from each asset");
                for (String assetId : assetIds) {
                    Set<String> elementIdsSet = getElementIdsSet(assetId);
                    if (elementIdsSet.size() > 0)
                        elementIds.addAll(elementIdsSet);
                }
            }
        } catch (Exception e) {
            log.error("Exception Caught in Getting Elements for Assets, errorMessage : {}", e.getMessage());
        }
        return elementIds;
    }

    private Set<String> getElementIdsSet(String assetId) {
        Set<String> elementsIdsSet = new HashSet<>();
        try {
            SonyCiListElementsForAssetsResponse sonyCiListElementsForAssetsResponse =
                    getElementsForAssets(1, 0, assetId);
            if (!Objects.isNull(sonyCiListElementsForAssetsResponse)) {
                long totalContent = sonyCiListElementsForAssetsResponse.getCount();
                int limit = 50;
                int offset = 0;
                int pages = (int) Math.ceil(totalContent / (double) limit);
                for (int i = 1; i <= pages; i++) {
                    sonyCiListElementsForAssetsResponse = getElementsForAssets(limit, offset, assetId);
                    Set<String> elementIdSetFiltered = addElementIdsToSet(sonyCiListElementsForAssetsResponse);
                    if (elementIdSetFiltered.size() > 0)
                        elementsIdsSet.addAll(elementIdSetFiltered);
                    offset += limit;
                }
            } else {
                log.info("No response received for elements details for assetId : {}", assetId);
            }
        } catch (Exception e) {
            log.error("Exception Caught in Getting Element ID's, errorMessage : {}", e.getMessage());
        }
        return elementsIdsSet;
    }

    private SonyCiListElementsForAssetsResponse getElementsForAssets(int limit, int offset, String assetId) {
        ResponseEntity<SonyCiListElementsForAssetsResponse> response;
        String param = "/" +
                assetId +
                "/elements" +
                "?limit=" + limit +
                "&" +
                "offset=" + offset +
                "&" +
                "orderBy=name&orderDirection=asc";
        String url = basicConfigProperties.getGetAllElementForAnAssetURL() + param;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpStatusCodeException exception) {
            log.error("Exception caught while fetching Elements for an asset: {}", exception.getMessage());
            return null;
        }
    }

    private Set<String> addElementIdsToSet(SonyCiListElementsForAssetsResponse sonyCiListElementsForAssetsResponse) {
        Set<String> elementsIdsSet = new HashSet<>();
        try {
            List<ItemsElements> items = sonyCiListElementsForAssetsResponse.getItems();
            for (ItemsElements item : items) {
                elementsIdsSet.add(item.getId());
            }
        }catch(Exception e){
            log.warn("Null Pointer Exception caught in getting items for getting element ids");
        }
        return elementsIdsSet;
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
        } catch (Exception exception) {
            log.error("Something went wrong while fetching details of workspace with id : {}, errorMessage : {}", workspaceId, exception.getMessage());
            return null;
        }
    }

}
