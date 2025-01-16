package com.adamnestor.courtvision.security.service;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.domain.UserRole;
import com.adamnestor.courtvision.domain.UserStatus;
import com.adamnestor.courtvision.repository.UsersRepository;
import com.adamnestor.courtvision.security.dto.AuthResponse;
import com.adamnestor.courtvision.security.dto.LoginRequest;
import com.adamnestor.courtvision.security.dto.RegisterRequest;
import com.adamnestor.courtvision.security.exception.EmailAlreadyExistsException;
import com.adamnestor.courtvision.security.exception.InvalidCredentialsException;
import com.adamnestor.courtvision.security.exception.PasswordMismatchException;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    @Mock
    private Authentication authentication;

    private AuthenticationService authenticationService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_TOKEN = "test.jwt.token";

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
    void whenRegisterWithValidData_thenSuccess() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                TEST_EMAIL,
                TEST_PASSWORD,
                TEST_PASSWORD
        );

        when(usersRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encodedPassword");
        when(jwtTokenUtil.generateToken(any())).thenReturn(TEST_TOKEN);
        when(usersRepository.save(any(Users.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        AuthResponse response = authenticationService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals(TEST_EMAIL, response.email());
        assertEquals(TEST_TOKEN, response.token());
        assertEquals(UserRole.USER, response.role());

        verify(usersRepository).existsByEmail(TEST_EMAIL);
        verify(passwordEncoder).encode(TEST_PASSWORD);
        verify(usersRepository).save(any(Users.class));
        verify(jwtTokenUtil).generateToken(any());
    }

    @Test
    void whenRegisterWithExistingEmail_thenThrowException() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                TEST_EMAIL,
                TEST_PASSWORD,
                TEST_PASSWORD
        );

        when(usersRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class,
                () -> authenticationService.register(request));

        verify(usersRepository).existsByEmail(TEST_EMAIL);
        verifyNoInteractions(passwordEncoder, jwtTokenUtil);
        verify(usersRepository, never()).save(any());
    }

    @Test
    void whenRegisterWithPasswordMismatch_thenThrowException() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                TEST_EMAIL,
                TEST_PASSWORD,
                "differentPassword"
        );

        // Act & Assert
        assertThrows(PasswordMismatchException.class,
                () -> authenticationService.register(request));

        verifyNoInteractions(passwordEncoder, jwtTokenUtil);
        verify(usersRepository, never()).save(any());
    }

    @Test
    void whenLoginWithValidCredentials_thenSuccess() {
        // Arrange
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        String encodedPassword = "encodedPassword";
        Users user = createTestUser();
        user.setPasswordHash(encodedPassword);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(usersRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(jwtTokenUtil.generateToken(any())).thenReturn(TEST_TOKEN);
        when(usersRepository.save(any(Users.class))).thenReturn(user);

        // Act
        AuthResponse response = authenticationService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals(TEST_EMAIL, response.email());
        assertEquals(TEST_TOKEN, response.token());
        assertEquals(UserRole.USER, response.role());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(usersRepository).findByEmail(TEST_EMAIL);
        verify(jwtTokenUtil).generateToken(any());
        verify(usersRepository).save(any(Users.class));
    }

    @Test
    void whenLoginWithInvalidCredentials_thenThrowException() {
        // Arrange
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.login(request));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtTokenUtil);
        verify(usersRepository, never()).save(any());
    }

    @Test
    void whenLoginWithNonexistentUser_thenThrowException() {
        // Arrange
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(usersRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.login(request));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(usersRepository).findByEmail(TEST_EMAIL);
        verifyNoInteractions(jwtTokenUtil);
        verify(usersRepository, never()).save(any());
    }

    private Users createTestUser() {
        Users user = new Users();
        user.setEmail(TEST_EMAIL);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        return user;
    }
}