package com.sesiones.sesiones_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SesionesBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SesionesBackendApplication.class, args);
	}

}

