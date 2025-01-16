package com.adamnestor.courtvision.security.jwt;

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
    private static final String SECRET = "thisIsAVeryLongSecretKeyForTestingPurposesOnly123456789";
    private static final long EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil(SECRET, EXPIRATION);
        userDetails = new User("test@example.com", "password", new ArrayList<>());
    }

    @Test
    void whenGenerateToken_thenSuccessful() {
        String token = jwtTokenUtil.generateToken(userDetails);
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void whenValidateToken_withValidToken_thenSuccess() {
        String token = jwtTokenUtil.generateToken(userDetails);
        assertTrue(jwtTokenUtil.validateToken(token));
    }

    @Test
    void whenValidateToken_withInvalidToken_thenFailure() {
        String invalidToken = "invalid.token.here";
        assertFalse(jwtTokenUtil.validateToken(invalidToken));
    }

    @Test
    void whenValidateTokenWithUserDetails_withValidToken_thenSuccess() {
        String token = jwtTokenUtil.generateToken(userDetails);
        assertTrue(jwtTokenUtil.validateToken(token, userDetails));
    }

    @Test
    void whenValidateTokenWithUserDetails_withWrongUser_thenFailure() {
        String token = jwtTokenUtil.generateToken(userDetails);
        UserDetails wrongUser = new User("wrong@example.com", "password", new ArrayList<>());
        assertFalse(jwtTokenUtil.validateToken(token, wrongUser));
    }

    @Test
    void whenGetEmailFromToken_thenSuccess() {
        String token = jwtTokenUtil.generateToken(userDetails);
        assertEquals("test@example.com", jwtTokenUtil.getEmailFromToken(token));
    }

    @Test
    void whenGetExpirationDateFromToken_thenNotExpired() {
        String token = jwtTokenUtil.generateToken(userDetails);
        Date expirationDate = jwtTokenUtil.getExpirationDateFromToken(token);

        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void whenValidateToken_withExpiredToken_thenFailure() throws InterruptedException {
        // Create token with very short expiration
        JwtTokenUtil shortExpirationJwt = new JwtTokenUtil(SECRET, 1); // 1ms expiration
        String token = shortExpirationJwt.generateToken(userDetails);

        // Wait for token to expire
        Thread.sleep(10);

        assertFalse(shortExpirationJwt.validateToken(token));
    }

    @Test
    void whenValidateToken_withMalformedToken_thenFailure() {
        String malformedToken = "eyJhbGciOiJIUzI1NiJ9.garbage.signature";
        assertFalse(jwtTokenUtil.validateToken(malformedToken));
    }
}