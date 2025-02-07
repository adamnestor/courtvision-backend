package com.adamnestor.courtvision.security.dto;

public record LogoutResponse(
    String message
) {
    public static LogoutResponse success() {
        return new LogoutResponse("Successfully logged out");
    }
} 