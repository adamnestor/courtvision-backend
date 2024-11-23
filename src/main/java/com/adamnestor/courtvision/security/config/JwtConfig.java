package com.adamnestor.courtvision.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties
public class JwtConfig {

    private final JwtProperties jwtProperties;

    public JwtConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil(
                jwtProperties.getSecret(),
                jwtProperties.getExpiration()
        );
    }
}

@ConfigurationProperties(prefix = "jwt")
class JwtProperties {
    private String secret;
    private Long expiration;

    // Getters and setters
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }
}