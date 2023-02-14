package com.hashedin.MetaDataExtraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetDto {
    private String id;
    private String name;
    private String size;
    private String format;
    private String status;
    private List<MetaDataFields> metadata;
}
