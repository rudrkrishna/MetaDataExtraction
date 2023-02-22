package com.hashedin.MetaDataExtraction.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
public class BasicConfigProperties {

    @Value("${sony.ci.bearer.token}")
    private String bearerToken;

    @Value("${sony.ci.addmetadata.url}")
    private String addMetaDataApi;

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

    @Value("${sony.ci.list.workspace.contents.url}")
    private String listWorkspaceContentsURL;

    @Value("${sony.ci.get.workspace.details.url}")
    private String getWorkspaceDetailsURL;

    @Value("${sony.ci.bulk.asset.or.elements}")
    private String bulkAssetOrElementDetails;

    @Value("${sony.ci.delete.metadata}")
    private String deleteMetaData;
}