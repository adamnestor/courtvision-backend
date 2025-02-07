package com.adamnestor.courtvision.security.dto;

import com.adamnestor.courtvision.domain.UserRole;

public record AuthResponse(
        String token,
        String email,
        UserRole role,
        String firstName,
        String lastName
) {
    // Static factory methods if needed
    public static AuthResponse success(String token, String email, UserRole role, String firstName, String lastName) {
        return new AuthResponse(token, email, role, firstName, lastName);
    }
}