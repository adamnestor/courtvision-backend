package com.adamnestor.courtvision.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:your-default-secret-key}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long jwtExpiration;

    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil(jwtSecret, jwtExpiration);
    }
}