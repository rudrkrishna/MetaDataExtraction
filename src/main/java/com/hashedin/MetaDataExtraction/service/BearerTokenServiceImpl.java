package com.hashedin.MetaDataExtraction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import com.hashedin.MetaDataExtraction.dto.SonyCiAccessRequest;
import com.hashedin.MetaDataExtraction.dto.SonyCiAccessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class BearerTokenServiceImpl {

    private final BasicConfigProperties properties;

    private final RestTemplate restTemplate;

    @Autowired
    public BearerTokenServiceImpl(BasicConfigProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public void setBearerToken() {
        ResponseEntity<SonyCiAccessResponse> responseEntity;
        try {
            final String URL = properties.getGenerateAccessTokenURL();
            SonyCiAccessRequest accessRequestDto = new SonyCiAccessRequest();
            accessRequestDto.setClient_id(properties.getClientId());
            accessRequestDto.setClient_secret(properties.getClientSecret());
            accessRequestDto.setGrant_type(properties.getGrantType());
            ObjectMapper mapper = new ObjectMapper();
            String jsonFormat = mapper.writeValueAsString(accessRequestDto);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setBasicAuth(properties.getBasicToken());
            HttpEntity<String> entity = new HttpEntity<>(jsonFormat, httpHeaders);
            log.info("Generating new bearer token");
            responseEntity = restTemplate.exchange(URL, HttpMethod.POST, entity, new ParameterizedTypeReference<>() { });
            properties.setBearerToken(responseEntity.getBody().getAccess_token());
        }
        catch (JsonProcessingException | NullPointerException exception) {
            log.info(exception.getMessage());
        }
    }

    @PostConstruct
    public void setUpBearerToken() {
        this.setBearerToken();
        log.info("bearer token generated successfully");
    }
}