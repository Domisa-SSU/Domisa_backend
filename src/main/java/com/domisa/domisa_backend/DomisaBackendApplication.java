package com.domisa.domisa_backend;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DomisaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DomisaBackendApplication.class, args);
	}

}
