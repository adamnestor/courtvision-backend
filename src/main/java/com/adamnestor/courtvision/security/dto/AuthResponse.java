package com.adamnestor.courtvision.security.dto;

import com.adamnestor.courtvision.domain.UserRole;

public record AuthResponse(
        String token,
        String email,
        UserRole role,
        String message
) {
    // Static factory methods for common responses
    public static AuthResponse success(String token, String email, UserRole role) {
        return new AuthResponse(token, email, role, "Authentication successful");
    }

    public static AuthResponse error(String message) {
        return new AuthResponse(null, null, null, message);
    }
}