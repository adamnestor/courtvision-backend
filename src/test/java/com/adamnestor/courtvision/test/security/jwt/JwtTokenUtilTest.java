package com.adamnestor.courtvision.test.security.jwt;

import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilTest {
    private JwtTokenUtil jwtTokenUtil;
    private UserDetails userDetails;
    private static final String SECRET = "testSecret123456789testSecret123456789testSecret123456789";
    private static final long EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil(SECRET, EXPIRATION);
        userDetails = User.withUsername("test@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        String token = jwtTokenUtil.generateToken(userDetails);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtTokenUtil.validateToken(token));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        String token = jwtTokenUtil.generateToken(userDetails);

        assertTrue(jwtTokenUtil.validateToken(token, userDetails));
    }

    @Test
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        JwtTokenUtil shortExpirationJwt = new JwtTokenUtil(SECRET, 0);
        String token = shortExpirationJwt.generateToken(userDetails);

        assertFalse(jwtTokenUtil.validateToken(token));
    }

    @Test
    void validateToken_WithInvalidSignature_ShouldReturnFalse() {
        String token = jwtTokenUtil.generateToken(userDetails);
        JwtTokenUtil differentSecretJwt = new JwtTokenUtil("differentSecret123456789differentSecret123456789", EXPIRATION);

        assertFalse(differentSecretJwt.validateToken(token));
    }

    @Test
    void getEmailFromToken_ShouldReturnCorrectEmail() {
        String token = jwtTokenUtil.generateToken(userDetails);
        String email = jwtTokenUtil.getEmailFromToken(token);

        assertEquals("test@example.com", email);
    }

    @Test
    void getExpirationDateFromToken_ShouldReturnCorrectDate() {
        String token = jwtTokenUtil.generateToken(userDetails);
        Date expiration = jwtTokenUtil.getExpirationDateFromToken(token);

        assertTrue(expiration.after(new Date()));
        assertTrue(expiration.before(new Date(System.currentTimeMillis() + EXPIRATION + 1000)));
    }

    @Test
    void validateToken_WithDifferentUser_ShouldReturnFalse() {
        String token = jwtTokenUtil.generateToken(userDetails);
        UserDetails differentUser = User.withUsername("different@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        assertFalse(jwtTokenUtil.validateToken(token, differentUser));
    }

    @Test
    void validateToken_WithMalformedToken_ShouldReturnFalse() {
        assertFalse(jwtTokenUtil.validateToken("malformed.token.here"));
    }
}