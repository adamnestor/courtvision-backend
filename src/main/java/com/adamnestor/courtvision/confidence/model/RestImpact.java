package com.adamnestor.courtvision.confidence.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents the impact of rest days on player performance.
 */
public class RestImpact {
    private final Integer daysOfRest;
    private final BigDecimal multiplier;
    private final BigDecimal impactScore;
    private final LocalDate gameDate;
    private final boolean isBackToBack;

    public RestImpact(Integer daysOfRest, BigDecimal multiplier, BigDecimal impactScore, LocalDate gameDate) {
        this.daysOfRest = daysOfRest;
        this.multiplier = multiplier;
        this.impactScore = impactScore;
        this.gameDate = gameDate;
        this.isBackToBack = daysOfRest != null && daysOfRest == 0;
    }

    public Integer getDaysOfRest() {
        return daysOfRest;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public BigDecimal getImpactScore() {
        return impactScore;
    }

    public LocalDate getGameDate() {
        return gameDate;
    }

    public boolean isBackToBack() {
        return isBackToBack;
    }

    @Override
    public String toString() {
        return "RestImpact{" +
                "daysOfRest=" + daysOfRest +
                ", multiplier=" + multiplier +
                ", impactScore=" + impactScore +
                ", gameDate=" + gameDate +
                ", isBackToBack=" + isBackToBack +
                '}';
    }
}