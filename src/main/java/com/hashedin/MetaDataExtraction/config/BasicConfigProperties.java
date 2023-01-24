package com.hashedin.MetaDataExtraction.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
@Data
public class BasicConfigProperties {

    @Value("${sony.ci.bearer.token}")
    private String bearerToken;
    @Value("${sony.ci.elememturl}")
    private String getUrl;
    @Value("${sony.ci.elementprops}")
    private String elemProp;
    @Value("${sony.ci.addmetadata.url}")
    private String addMetaDataApi;
    @Value("${sony.ci.assetid}")
    private String assetId;
    @Value("${sony.ci.get.access.token.url}")
    private String generateAccessTokenURL;
    @Value("${sony.ci.basic.token}")
    private String basicToken;
    @Value("${sony.ci.client.id}")
    private String clientId;
    @Value("${sony.ci.client.secret}")
    private String clientSecret;
    @Value(("${sony.ci.grant.type}"))
    private String grantType;
}