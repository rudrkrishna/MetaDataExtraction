package com.hashedin.MetaDataExtraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SonyCiListElementsForAssetsResponse {
    private int limit;
    private int offset;
    private int count;
    private List<ItemsElements> items;
}

