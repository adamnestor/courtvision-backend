package com.adamnestor.courtvision.dto.common;

public record ApiResponse<T>(
        T data,
        String message,
        boolean success
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, "Success", true);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(null, message, false);
    }
}