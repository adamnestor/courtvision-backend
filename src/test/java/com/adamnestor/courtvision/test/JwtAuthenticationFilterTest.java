package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.security.jwt.JwtAuthenticationFilter;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import com.adamnestor.courtvision.security.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private TestJwtAuthenticationFilter jwtAuthenticationFilter;
    private UserDetails userDetails;

    // Test-specific subclass that exposes the protected method
    private static class TestJwtAuthenticationFilter extends JwtAuthenticationFilter {
        public TestJwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, UserDetailsServiceImpl userDetailsService) {
            super(jwtTokenUtil, userDetailsService);
        }

        @Override
        public void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
            super.doFilterInternal(request, response, filterChain);
        }
    }

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new TestJwtAuthenticationFilter(jwtTokenUtil, userDetailsService);
        userDetails = new User("test@example.com", "password", Collections.emptyList());
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenValidToken_thenAuthenticationShouldBeSet() throws IOException, ServletException {
        // Arrange
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(jwtTokenUtil.getEmailFromToken(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken(token, userDetails)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService).loadUserByUsername("test@example.com");
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("test@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void whenNoToken_thenContinueFilterChain() throws IOException, ServletException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void whenInvalidTokenFormat_thenContinueFilterChain() throws IOException, ServletException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void whenTokenValidationFails_thenContinueFilterChain() throws IOException, ServletException {
        // Arrange
        String token = "invalid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtil.validateToken(token)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void whenUserValidationFails_thenContinueFilterChain() throws IOException, ServletException {
        // Arrange
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(jwtTokenUtil.getEmailFromToken(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken(token, userDetails)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService).loadUserByUsername("test@example.com");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void whenExceptionOccurs_thenContinueFilterChain() throws IOException, ServletException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtTokenUtil.validateToken(anyString())).thenThrow(new RuntimeException("Token processing error"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}