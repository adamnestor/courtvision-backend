package com.adamnestor.courtvision.confidence.model;

import java.math.BigDecimal;

/**
 * Represents the calculated risk of a blowout occurring in a game
 */
public class BlowoutImpact {
    private final BigDecimal strengthDifferential;
    private final BigDecimal matchupFactor;
    private final BigDecimal totalRisk;

    public BlowoutImpact(
            BigDecimal strengthDifferential,
            BigDecimal matchupFactor,
            BigDecimal totalRisk) {
        this.strengthDifferential = strengthDifferential;
        this.matchupFactor = matchupFactor;
        this.totalRisk = totalRisk;
    }

    public BigDecimal getStrengthDifferential() {
        return strengthDifferential;
    }

    public BigDecimal getMatchupFactor() {
        return matchupFactor;
    }

    public BigDecimal getTotalRisk() {
        return totalRisk;
    }

    @Override
    public String toString() {
        return "BlowoutImpact{" +
                "strengthDifferential=" + strengthDifferential +
                ", matchupFactor=" + matchupFactor +
                ", totalRisk=" + totalRisk +
                '}';
    }
}