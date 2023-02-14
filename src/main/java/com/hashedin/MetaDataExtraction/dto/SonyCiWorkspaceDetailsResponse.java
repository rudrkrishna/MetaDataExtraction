package com.hashedin.MetaDataExtraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SonyCiWorkspaceDetailsResponse {
    private String name;
    private String id;
    private String rootFolderId;
}
