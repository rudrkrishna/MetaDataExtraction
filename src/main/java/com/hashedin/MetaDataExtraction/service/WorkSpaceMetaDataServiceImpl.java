package com.hashedin.MetaDataExtraction.service;

import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import com.hashedin.MetaDataExtraction.dto.*;
import com.hashedin.MetaDataExtraction.repository.ElementsRepository;
import com.hashedin.MetaDataExtraction.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class WorkSpaceMetaDataServiceImpl {
    private final BasicConfigProperties basicConfigProperties;
    private final RestTemplate restTemplate;
    private final BearerTokenServiceImpl bearerTokenService;
    private final MetaDataServiceImpl metaDataService;

    @Autowired
    public WorkSpaceMetaDataServiceImpl(BasicConfigProperties basicConfigProperties,
                                        RestTemplate restTemplate, BearerTokenServiceImpl bearerTokenService,
                                        MetaDataServiceImpl metaDataService) {
        this.basicConfigProperties = basicConfigProperties;
        this.restTemplate = restTemplate;
        this.bearerTokenService = bearerTokenService;
        this.metaDataService = metaDataService;
    }

    public List<String> listWorkspaceContents() {
        SonyCiListWorkspaceContentsResponse listWorkspaceContentsResponseDto =
                listWorkspaceContents(1, 0);
        List<String> elementIds=new ArrayList<>();
        try{
        long totalContent = listWorkspaceContentsResponseDto.getCount();
        int limit = 100;
        int offset = 0;
        int pages = (int) Math.ceil(totalContent / (double) limit);
        List<String> assetIds= new ArrayList<>();
        for (int i = 1; i <= pages; i++) {
            listWorkspaceContentsResponseDto = listWorkspaceContents(limit, offset);
            workspaceContents(listWorkspaceContentsResponseDto, assetIds);
            offset += limit;
        }
        elementIds=getElemetsForAssets(assetIds);
        }catch(Exception e){
         log.warn("Null Pointer Exception caught in getting Content Count");
        }
        return elementIds;
    }

    private SonyCiListWorkspaceContentsResponse listWorkspaceContents(int limit, int offset) {
        ResponseEntity<SonyCiListWorkspaceContentsResponse> responseEntity;
        String param = basicConfigProperties.getWorkspaceId() +
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
                    new ParameterizedTypeReference<>() {});
        }
        catch(HttpStatusCodeException exception) {
            log.error(exception.getMessage());
            bearerTokenService.setBearerToken();
            httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
            responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<>() {});
        }

        return responseEntity.getBody();
    }

    private void workspaceContents(SonyCiListWorkspaceContentsResponse listWorkspaceContentsResponseDto, List<String> assetIds) {
        try {
            List<Items> itemsList = listWorkspaceContentsResponseDto.getItems();
            log.info("All Items fetched from Workspace");
            for (Items item : itemsList) {
                if (item.getKind().equalsIgnoreCase(Constants.ASSET))
                {
                    assetIds.add(item.getId());
                }
            }
        }catch(Exception e){
            log.warn("Exception caught in getting Items");
            log.error("Error Cause: "+e.getCause());
        }
    }



    private List<String> getElemetsForAssets(List<String> assetIds){
        List<String> elementIds= new ArrayList<>();
        try {
            log.info("Element Id's are being fetched from each asset");
            Iterator<String> it = assetIds.iterator();
            Set<String> elem = new HashSet<>();
            while (it.hasNext()) {
                elem = getElementIdsSet(it.next());
                if (elem.size() != 0)
                    elementIds.addAll(elem);
            }
        }catch(RuntimeException e){
            log.info("Exception Caught in Getting Elements for Assets");
            log.error("Error Message: "+e.getMessage());
        }
        return elementIds;
    }

    private Set<String> getElementIdsSet(String assetId) {
        Set<String> elementsIdsSet = new HashSet<>();
        try {
            SonyCiListElementsForAssetsResponse sonyCiListElementsForAssetsResponse =
                    getElementsForAssets(1, 0, assetId);
            long totalContent = sonyCiListElementsForAssetsResponse.getCount();
            int limit = 50;
            int offset = 0;
            int pages = (int) Math.ceil(totalContent / (double) limit);
            for (int i = 1; i <= pages; i++) {
                sonyCiListElementsForAssetsResponse = getElementsForAssets(limit, offset, assetId);
                addElementIdsToSet(sonyCiListElementsForAssetsResponse, elementsIdsSet);
                offset += limit;
            }
        }catch(RuntimeException e){
            log.info("Exception Caught in Getting Element ID's");
            log.error("Error Message"+e.getMessage());
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
                    new ParameterizedTypeReference<>() {});
        }
        catch(HttpStatusCodeException exception) {
            log.error(exception.getMessage());
            bearerTokenService.setBearerToken();
            httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
            response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<>() {});
        }
        return response.getBody();
    }

    private void addElementIdsToSet(SonyCiListElementsForAssetsResponse sonyCiListElementsForAssetsResponse,
                                    Set<String> elementIdsSet) {
        try {
            List<ItemsElements> items = sonyCiListElementsForAssetsResponse.getItems();
            Iterator<ItemsElements> it=items.iterator();
            while(it.hasNext()) {
                elementIdsSet.add(it.next().getId());
            }
        }catch(Exception e){
            log.warn("Null Pointer Exception caught in getting items for getting element ids");
        }
    }


    public void deleteMetaData(List<MetaDataFields> li, String assetId){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
        try {
            for (MetaDataFields m : li) {
                String url = basicConfigProperties.getAddMetaDataApi() + assetId + "/metadata/" + m.getName();
                restTemplate.exchange(url, HttpMethod.DELETE, entity, new ParameterizedTypeReference<>() {
                });
            }
            log.info("MetaData Deletion Successful");
        }catch(Exception e){
            log.info("MetaData Fields does not exist");
        }
    }

    public void migrateMetaData() {

        List<String> elementIds= listWorkspaceContents();
        Iterator<String> it=null;
        try{
         it= elementIds.iterator();
        while(it.hasNext()){
            String s=it.next();
            ElementResponse response = metaDataService.getDownloadableUrl(s);
            if (metaDataService.isXmlFile(response.getName())) {
                metaDataService.addMetaData(metaDataService.fetchMetaDataFields(response), response.getAsset().getId());
            }
        }
        }catch(Exception e){
            log.info("Exception Caught while Iterating List of Element ID's");
            log.error("Error Cause"+e.getCause());
        }


    }

    public void deleteData() {
        List<String> elementIds= listWorkspaceContents();
        Iterator<String> it=null;
        try {
            if(elementIds.size()!=0) {
                it = elementIds.iterator();
                while (it.hasNext()) {
                    String s = it.next();
                    ElementResponse response = metaDataService.getDownloadableUrl(s);
                    if (metaDataService.isXmlFile(response.getName())) {
                        deleteMetaData(metaDataService.fetchMetaDataFields(response), response.getAsset().getId());
                    }
                }
            }
        }catch(Exception e){
            log.info("Exception Caught while Iterating List of Element ID's");
            log.error("Error Cause"+e.getCause());
        }
    }
}
