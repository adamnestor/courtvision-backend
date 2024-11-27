package com.adamnestor.courtvision.test.security.dto;

import com.adamnestor.courtvision.security.dto.LoginRequest;
import com.adamnestor.courtvision.security.dto.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthDtoTests {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenLoginRequestValid_thenNoViolations() {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        var violations = validator.validate(loginRequest);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenLoginRequestInvalid_thenViolations() {
        LoginRequest loginRequest = new LoginRequest("", "");
        var violations = validator.validate(loginRequest);
        assertEquals(2, violations.size());
    }

    @Test
    void whenRegisterRequestValid_thenNoViolations() {
        RegisterRequest registerRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "password123"
        );
        var violations = validator.validate(registerRequest);
        assertTrue(violations.isEmpty());
        assertTrue(registerRequest.isPasswordMatch());
    }

    @Test
    void whenPasswordsDontMatch_thenFalse() {
        RegisterRequest registerRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "password456"
        );
        assertFalse(registerRequest.isPasswordMatch());
    }
}