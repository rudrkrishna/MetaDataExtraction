package com.hashedin.MetaDataExtraction;

import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class MetaDataExtractionApplication {

	public static void main(String[] args) {
		SpringApplication.run(MetaDataExtractionApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	@Bean
	public BasicConfigProperties basicConfigProperties(){
		return new BasicConfigProperties();
	}

}
