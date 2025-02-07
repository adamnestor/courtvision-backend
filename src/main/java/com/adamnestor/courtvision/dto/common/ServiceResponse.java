package com.adamnestor.courtvision.dto.common;

public record ServiceResponse<T>(
        T data,
        String message,
        boolean success,
        Object metadata
) {
    public static <T> ServiceResponse<T> success(T data) {
        return new ServiceResponse<>(data, "Success", true, null);
    }

    public static <T> ServiceResponse<T> success(T data, Object metadata) {
        return new ServiceResponse<>(data, "Success", true, metadata);
    }

    public static <T> ServiceResponse<T> error(String message) {
        return new ServiceResponse<>(null, message, false, null);
    }
}