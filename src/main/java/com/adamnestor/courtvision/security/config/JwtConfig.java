package com.adamnestor.courtvision.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class JwtConfig {

    @Autowired
    private JwtProperties jwtProperties;

    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil(
                jwtProperties.getSecret(),
                jwtProperties.getExpiration()
        );
    }
}