package com.hashedin.MetaDataExtraction;

import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import com.hashedin.MetaDataExtraction.controller.MetadataController;
import com.hashedin.MetaDataExtraction.controller.WorkspaceMeteDataController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootApplication
@EnableScheduling
@Import({ MetadataController.class })
public class MetaDataExtractionApplication {

	/*
	 * Create required HandlerMapping, to avoid several default HandlerMapping instances being created
	 */
	/*@Bean
	public HandlerMapping handlerMapping() {
		return new RequestMappingHandlerMapping();
	}*/

	/*
	 * Create required HandlerAdapter, to avoid several default HandlerAdapter instances being created
	 */
	/* @Bean
	public HandlerAdapter handlerAdapter() {
		return new RequestMappingHandlerAdapter();
	}*/


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