package com.hashedin.MetaDataExtraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Items {
    private String id;
    private String name;
    private ParentFolder parentFolder;
    private String kind;
    private Folder folder;
    private String status;
    private List<MetaDataFields> metadata;
}
