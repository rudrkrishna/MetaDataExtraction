package com.hashedin.MetaDataExtraction.dto;

import lombok.Data;

import java.util.List;

@Data
public class BulkDetailsPayload {
    public List<String> assetIds;
    public List<String> elementIds;
    public int limit = 50;
    public int offset = 0;
    public String orderBy;
    public String orderDirection;
}
