package com.hashedin.MetaDataExtraction.dto;

import lombok.Data;

import java.util.Set;

@Data
public class BulkDetailsPayload {
    public Set<String> assetIds;
    public Set<String> elementIds;
    public int limit = 50;
    public int offset = 0;
    public String orderBy;
    public String orderDirection;
}
