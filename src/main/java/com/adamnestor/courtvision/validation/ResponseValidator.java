package com.adamnestor.courtvision.validation;

import com.adamnestor.courtvision.dto.response.PlayerStatsResponse;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class ResponseValidator {

    public void validatePlayerStatsResponse(PlayerStatsResponse response) {
        if (response.confidenceScore() < 0 || response.confidenceScore() > 100) {
            throw new IllegalArgumentException("Confidence score must be between 0 and 100");
        }
        if (response.hitRate().compareTo(new BigDecimal("0")) < 0 || 
            response.hitRate().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Hit rate must be between 0 and 100");
        }
    }

    // Similar validation methods for other response types
} 