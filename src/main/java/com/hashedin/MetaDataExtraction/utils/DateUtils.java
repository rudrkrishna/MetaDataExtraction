package com.hashedin.MetaDataExtraction.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DateUtils {

    public String expiryDate = "?downloadExpirationDate="+LocalDateTime.now().plusDays(1).toString()+"Z";

}
