package com.adamnestor.courtvision.security.dto;

import com.adamnestor.courtvision.domain.UserRole;

public record AuthResponse(
        String token,
        String email,
        UserRole role
) {
    // Static factory methods if needed
    public static AuthResponse success(String token, String email, UserRole role) {
        return new AuthResponse(token, email, role);
    }
}