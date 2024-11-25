package com.adamnestor.courtvision.security.service;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.domain.UserRole;
import com.adamnestor.courtvision.repository.UsersRepository;
import com.adamnestor.courtvision.security.dto.AuthResponse;
import com.adamnestor.courtvision.security.dto.RegisterRequest;
import com.adamnestor.courtvision.security.exception.EmailAlreadyExistsException;
import com.adamnestor.courtvision.security.exception.InvalidCredentialsException;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}