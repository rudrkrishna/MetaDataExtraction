package com.hashedin.MetaDataExtraction.schedulers;

import com.hashedin.MetaDataExtraction.service.BearerTokenServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
@Configuration
@Slf4j
public class SonyCiScheduler {
    @Autowired
    BearerTokenServiceImpl bearerTokenService;

    @Scheduled(cron = "0 0 */12 * * *")
    public void generateToken() {
        bearerTokenService.setBearerToken();
        log.info("bearer token generated successfully");
    }
}
