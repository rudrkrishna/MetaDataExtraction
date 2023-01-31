package com.hashedin.MetaDataExtraction.service;

import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import com.hashedin.MetaDataExtraction.dto.Items;
import com.hashedin.MetaDataExtraction.dto.ItemsElements;
import com.hashedin.MetaDataExtraction.dto.SonyCiListElementsForAssetsResponse;
import com.hashedin.MetaDataExtraction.dto.SonyCiListWorkspaceContentsResponse;
import com.hashedin.MetaDataExtraction.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class WorkSpaceMetaDataServiceImpl {

    private final BasicConfigProperties basicConfigProperties;
    private final RestTemplate restTemplate;
    private final BearerTokenServiceImpl bearerTokenService;
    private static List<String> assetIds= new ArrayList<>();
    private static List<String> elementIds= new ArrayList<>();

    @Autowired
    public WorkSpaceMetaDataServiceImpl(BasicConfigProperties basicConfigProperties,
                                        RestTemplate restTemplate, BearerTokenServiceImpl bearerTokenService) {
        this.basicConfigProperties = basicConfigProperties;
        this.restTemplate = restTemplate;
        this.bearerTokenService = bearerTokenService;
    }

    public List<String> listWorkspaceContents() {
        SonyCiListWorkspaceContentsResponse listWorkspaceContentsResponseDto =
                listWorkspaceContents(1, 0);
        long totalContent = listWorkspaceContentsResponseDto.getCount();
        int limit = 100;
        int offset = 0;
        int pages = (int) Math.ceil(totalContent / (double) limit);
        for (int i = 1; i <= pages; i++) {
            listWorkspaceContentsResponseDto = listWorkspaceContents(limit, offset);
            workspaceContents(listWorkspaceContentsResponseDto);
            offset += limit;
        }
        getElemetsForAssets();
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

    private void workspaceContents(SonyCiListWorkspaceContentsResponse listWorkspaceContentsResponseDto) {
        List<Items> itemsList = listWorkspaceContentsResponseDto.getItems();
        log.info("All Items fetched from Workspace");
        for (Items item : itemsList) {
            log.info(item.getName());
            fetchAsset(item);
        }
    }

    private void fetchAsset(Items item) {
        if (item.getKind().equalsIgnoreCase(Constants.ASSET))
        {
            assetIds.add(item.getId());
        }
    }

    private void getElemetsForAssets(){
        for(String str:assetIds){
            elementIds.addAll(getElementIdsSet(str));
        }
    }

    private Set<String> getElementIdsSet(String assetId) {
        SonyCiListElementsForAssetsResponse sonyCiListElementsForAssetsResponse =
                getElementsForAssets(1, 0, assetId);
        long totalContent = sonyCiListElementsForAssetsResponse.getCount();
        int limit = 50;
        int offset = 0;
        int pages = (int) Math.ceil(totalContent / (double) limit);
        Set<String> elementsIdsSet = new HashSet<>();
        for (int i = 1; i <= pages; i++) {
            sonyCiListElementsForAssetsResponse = getElementsForAssets(limit, offset, assetId);
            addElementIdsToSet(sonyCiListElementsForAssetsResponse, elementsIdsSet);
            offset += limit;
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
        List<ItemsElements> items = sonyCiListElementsForAssetsResponse.getItems();
        for(ItemsElements item : items) {
            elementIdsSet.add(item.getId());
        }
    }

    public void flushDS() {
        assetIds.clear();
        elementIds.clear();

    }
}
