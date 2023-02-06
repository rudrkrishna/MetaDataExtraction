package com.hashedin.MetaDataExtraction;

import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableScheduling
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