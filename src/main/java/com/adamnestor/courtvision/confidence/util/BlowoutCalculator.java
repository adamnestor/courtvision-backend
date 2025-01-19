package com.adamnestor.courtvision.confidence.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BlowoutCalculator {
    private static final int SCALE = 4;
    private static final int BLOWOUT_THRESHOLD = 20;
    private static final BigDecimal HOME_ADVANTAGE = new BigDecimal("1.5"); // Reduced from 2.5
    private static final BigDecimal PACE_WEIGHT = new BigDecimal("0.01"); // Reduced from 0.02

    private BlowoutCalculator() {
        throw new IllegalStateException("Utility class");
    }

    public static BigDecimal calculateTeamStrengthDifferential(
            BigDecimal homeNetRating,
            BigDecimal awayNetRating,
            BigDecimal homePace,
            BigDecimal awayPace) {

        // Calculate net rating differential
        BigDecimal netRatingDiff = homeNetRating.subtract(awayNetRating);

        // Calculate pace impact with minimal weight
        BigDecimal avgPace = homePace.add(awayPace)
                .divide(new BigDecimal("2"), SCALE, RoundingMode.HALF_UP);
        BigDecimal paceImpact = avgPace.subtract(new BigDecimal("100.00"))
                .multiply(PACE_WEIGHT);

        // Add reduced home court advantage
        return netRatingDiff
                .add(paceImpact)
                .add(HOME_ADVANTAGE)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static boolean wasBlowout(Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null) {
            return false;
        }
        return Math.abs(homeScore - awayScore) >= BLOWOUT_THRESHOLD;
    }

    public static BigDecimal calculateBlowoutProbability(BigDecimal strengthDifferential) {
        // Base probability starts at 25% for perfectly even teams
        double baseProbability = 25.0;

        // Each point of strength differential adds 1.5% to probability
        double differentialImpact = strengthDifferential.doubleValue() * 1.5;

        // Add the differential impact to base probability
        double totalProbability = baseProbability + differentialImpact;

        // Ensure probability stays within reasonable bounds
        totalProbability = Math.min(85.0, Math.max(15.0, totalProbability));

        return BigDecimal.valueOf(totalProbability)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateHistoricalMatchupFactor(int blowoutGames, int totalGames) {
        if (totalGames == 0) {
            return BigDecimal.ONE;
        }

        BigDecimal blowoutRate = BigDecimal.valueOf(blowoutGames)
                .divide(BigDecimal.valueOf(totalGames), SCALE, RoundingMode.HALF_UP);

        // Reduced maximum adjustment to 5%
        return BigDecimal.ONE.add(blowoutRate.multiply(new BigDecimal("0.05")));
    }
}