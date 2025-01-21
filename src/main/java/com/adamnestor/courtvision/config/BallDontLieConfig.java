package com.adamnestor.courtvision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BallDontLieConfig {
    
    @Value("${balldontlie.api-key}")
    private String apiKey;
    
    @Value("${balldontlie.base-url}")
    private String baseUrl;
    
    @Bean
    public WebClient ballDontLieClient() {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", apiKey)
            .build();
    }
} 