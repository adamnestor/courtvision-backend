package com.adamnestor.courtvision.security.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, RegisterRequest> {
    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        // Initialization code if needed
    }

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Let @NotNull handle null values
        }

        if (request.password() == null || request.confirmPassword() == null) {
            return false;
        }

        return request.password().equals(request.confirmPassword());
    }
}