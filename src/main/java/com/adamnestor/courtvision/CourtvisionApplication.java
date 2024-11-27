package com.adamnestor.courtvision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class CourtvisionApplication {
	public static void main(String[] args) {
		SpringApplication.run(CourtvisionApplication.class, args);
	}
}