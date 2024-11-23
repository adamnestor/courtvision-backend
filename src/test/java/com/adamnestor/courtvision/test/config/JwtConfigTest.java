package com.adamnestor.courtvision.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class JwtConfigTest {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void contextLoads() {
        assertNotNull(jwtTokenUtil, "JwtTokenUtil should be autowired successfully");
        assertNotNull(jwtProperties, "JwtProperties should be autowired successfully");
        assertNotNull(jwtProperties.getSecret(), "JWT secret should be loaded from properties");
        assertNotNull(jwtProperties.getExpiration(), "JWT expiration should be loaded from properties");
    }
}