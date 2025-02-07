package com.adamnestor.courtvision.confidence.model;

import java.math.BigDecimal;

public class RestImpact {
    private final int daysOfRest;
    private final BigDecimal multiplier;

    public RestImpact(int daysOfRest) {
        this.daysOfRest = daysOfRest;
        this.multiplier = calculateMultiplier();
    }

    private BigDecimal calculateMultiplier() {
        return switch (daysOfRest) {
            case 0 -> new BigDecimal("0.93"); // Back-to-back
            case 1 -> BigDecimal.ONE;         // Normal rest
            default -> new BigDecimal("1.02"); // Extended rest
        };
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }
}
