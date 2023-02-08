package com.hashedin.MetaDataExtraction.schedulers;

import com.hashedin.MetaDataExtraction.service.BearerTokenServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
@Configuration
public class SonyCiScheduler {
    @Autowired
    BearerTokenServiceImpl bearerTokenService;

    @Scheduled(cron = "0 0 */12 * * *")
    public void generateToken() {
        bearerTokenService.setBearerToken();
    }
}
