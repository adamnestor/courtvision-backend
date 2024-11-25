package com.adamnestor.courtvision.test.security.integration;

import com.adamnestor.courtvision.domain.UserRole;
import com.adamnestor.courtvision.repository.UsersRepository;
import com.adamnestor.courtvision.security.dto.AuthResponse;
import com.adamnestor.courtvision.security.dto.LoginRequest;
import com.adamnestor.courtvision.security.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        usersRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        usersRepository.deleteAll();
    }

    @Test
    void whenFullRegistrationAndLoginFlow_thenSuccess() throws Exception {
        // Register
        RegisterRequest registerRequest = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, TEST_PASSWORD);
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse registerResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertNotNull(registerResponse.token());
        assertEquals(TEST_EMAIL, registerResponse.email());
        assertEquals(UserRole.USER, registerResponse.role());

        // Login
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertNotNull(loginResponse.token());

        // Access Protected Endpoint
        mockMvc.perform(get("/api/picks")
                        .header("Authorization", "Bearer " + loginResponse.token()))
                .andExpect(status().isOk());
    }

    @Test
    void whenRegisterWithExistingEmail_thenFails() throws Exception {
        // Register first user
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, TEST_PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Try to register again with same email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void whenLoginWithInvalidCredentials_thenFails() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, TEST_PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAccessingProtectedEndpointWithoutToken_thenFails() throws Exception {
        mockMvc.perform(get("/api/picks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAccessingProtectedEndpointWithInvalidToken_thenFails() throws Exception {
        mockMvc.perform(get("/api/picks")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAccessingPublicEndpoint_thenSucceeds() throws Exception {
        mockMvc.perform(get("/api/public/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void whenRegisterWithoutConfirmPassword_thenFails() throws Exception {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, "differentpassword");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenLoginWithNonexistentEmail_thenFails() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent@example.com", TEST_PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}