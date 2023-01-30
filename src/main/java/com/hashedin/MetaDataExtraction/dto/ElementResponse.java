

package com.hashedin.MetaDataExtraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElementResponse {
    String id;
    Asset asset;
    long size;
    String downloadUrl;
    String name;
    String createdOn;
    String md5Checksum;
    MetaDataFormat metaData;
    String status;
    List<String> customKeys;
    boolean isLocked;
    LastLockActionBy lastLockActionBy;
    String lastLockActionOn;
    boolean isExternal;

}