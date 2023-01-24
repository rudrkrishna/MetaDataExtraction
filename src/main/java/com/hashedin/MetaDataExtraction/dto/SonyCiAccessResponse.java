package com.hashedin.MetaDataExtraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SonyCiAccessResponse {
    private String access_token;
    private int expires_in;
    private String token_type;
    private String refresh_token;
}
