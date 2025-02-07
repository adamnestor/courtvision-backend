package com.adamnestor.courtvision.exception;

public class ApiRateLimitException extends ApiException {
    public ApiRateLimitException(String message) {
        super(message);
    }

    public ApiRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
} 