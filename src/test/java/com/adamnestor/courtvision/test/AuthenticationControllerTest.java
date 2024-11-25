package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.UserRole;
import com.adamnestor.courtvision.security.controller.AuthenticationController;
import com.adamnestor.courtvision.security.dto.AuthResponse;
import com.adamnestor.courtvision.security.dto.LoginRequest;
import com.adamnestor.courtvision.security.dto.RegisterRequest;
import com.adamnestor.courtvision.security.exception.EmailAlreadyExistsException;
import com.adamnestor.courtvision.security.exception.InvalidCredentialsException;
import com.adamnestor.courtvision.security.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void whenValidRegisterRequest_thenReturns200() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "password123",
                "password123"
        );

        AuthResponse response = new AuthResponse(
                "jwt.token.here",
                "test@example.com",
                UserRole.USER
        );

        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void whenEmailExists_thenReturns409() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "password123",
                "password123"
        );

        when(authenticationService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException());

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void whenValidLoginRequest_thenReturns200() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        AuthResponse response = new AuthResponse(
                "jwt.token.here",
                "test@example.com",
                UserRole.USER
        );

        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void whenInvalidCredentials_thenReturns401() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "wrongpassword"
        );

        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenInvalidRequest_thenReturns400() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "", // invalid email
                "pwd",  // too short password
                "pwd"
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}