package com.floweapp.flowe_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FloweApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FloweApiApplication.class, args);
	}

}
