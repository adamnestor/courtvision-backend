package com.adamnestor.courtvision.confidence.model;

import java.math.BigDecimal;

/**
 * Represents the impact analysis of blowouts on player performance
 */
public class BlowoutImpact {
    private final BigDecimal minutesRetention;
    private final BigDecimal performanceRetention;
    private final BigDecimal baseRisk;

    public BlowoutImpact(
            BigDecimal minutesRetention,
            BigDecimal performanceRetention,
            BigDecimal baseRisk) {
        this.minutesRetention = minutesRetention;
        this.performanceRetention = performanceRetention;
        this.baseRisk = baseRisk;
    }

    public BigDecimal getMinutesRetention() {
        return minutesRetention;
    }

    public BigDecimal getPerformanceRetention() {
        return performanceRetention;
    }

    public BigDecimal getBaseRisk() {
        return baseRisk;
    }

    @Override
    public String toString() {
        return "BlowoutImpact{" +
                "minutesRetention=" + minutesRetention +
                ", performanceRetention=" + performanceRetention +
                ", baseRisk=" + baseRisk +
                '}';
    }
}