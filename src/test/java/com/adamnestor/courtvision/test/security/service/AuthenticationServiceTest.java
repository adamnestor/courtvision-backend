package com.adamnestor.courtvision.test.security.service;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.domain.UserRole;
import com.adamnestor.courtvision.repository.UsersRepository;
import com.adamnestor.courtvision.security.dto.AuthResponse;
import com.adamnestor.courtvision.security.dto.LoginRequest;
import com.adamnestor.courtvision.security.dto.RegisterRequest;
import com.adamnestor.courtvision.security.exception.EmailAlreadyExistsException;
import com.adamnestor.courtvision.security.exception.InvalidCredentialsException;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import com.adamnestor.courtvision.security.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                usersRepository,
                passwordEncoder,
                jwtTokenUtil,
                authenticationManager
        );
    }

    @Test
    void whenValidRegistration_thenSuccess() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "password123",
                "password123"
        );

        when(usersRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(jwtTokenUtil.generateToken(any(UserDetails.class))).thenReturn("jwt.token.here");

        Users savedUser = new Users();
        savedUser.setEmail(request.email());
        savedUser.setPasswordHash("hashedPassword");
        savedUser.setRole(UserRole.USER);
        when(usersRepository.save(any(Users.class))).thenReturn(savedUser);

        // Act
        AuthResponse response = authenticationService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals(request.email(), response.email());
        assertEquals("jwt.token.here", response.token());
        assertEquals(UserRole.USER, response.role());  // Now comparing with enum
    }

    @Test
    void whenEmailExists_thenThrowException() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "password123",
                "password123"
        );

        when(usersRepository.existsByEmail(request.email())).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () ->
                authenticationService.register(request));
    }

    @Test
    void whenPasswordsDontMatch_thenThrowException() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "password123",
                "password456"
        );

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () ->
                authenticationService.register(request));
    }

    @Test
    void whenValidLogin_thenSuccess() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        // Mock authentication success
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                request.email(), request.password());
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        // Mock user retrieval
        Users user = new Users();
        user.setEmail(request.email());
        user.setPasswordHash("hashedPassword");
        user.setRole(UserRole.USER);
        when(usersRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(jwtTokenUtil.generateToken(any(UserDetails.class)))
                .thenReturn("jwt.token.here");

        // Act
        AuthResponse response = authenticationService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals(request.email(), response.email());
        assertEquals("jwt.token.here", response.token());
        assertEquals(UserRole.USER, response.role());
    }

    @Test
    void whenInvalidCredentials_thenThrowException() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new AuthenticationException("Invalid credentials") {});

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () ->
                authenticationService.login(request));
    }

    @Test
    void whenUserNotFound_thenThrowException() {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        // Mock authentication success but user not found in database
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                request.email(), request.password());
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        when(usersRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () ->
                authenticationService.login(request));
    }
}