package com.hashedin.MetaDataExtraction.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SonyCiListWorkspaceContentsResponse {
    private int limit;
    private int offset;
    private long count;
    private String kind;
    private Order order;
    private List<Items> items;
}
