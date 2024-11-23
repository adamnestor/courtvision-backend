package com.adamnestor.courtvision.security.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.*;

// Custom annotation definition
@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Target({ElementType.TYPE})  // Annotation can be applied to classes
@Retention(RetentionPolicy.RUNTIME)  // Annotation will be retained at runtime
@interface PasswordMatch {
    String message() default "Passwords do not match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator implementation
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, RegisterRequest> {

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        // Initialization code if needed
    }

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        // Return true if the passwords match, false otherwise
        if (request == null) {
            return true; // Let @NotNull handle null values
        }

        // Check if both password fields are present
        if (request.password() == null || request.confirmPassword() == null) {
            return false;
        }

        // Return true if passwords match
        return request.password().equals(request.confirmPassword());
    }
}