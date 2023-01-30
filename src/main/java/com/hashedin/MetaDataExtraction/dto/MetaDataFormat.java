package com.hashedin.MetaDataExtraction.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetaDataFormat {
    List<MetaDataFields> metaData;
}