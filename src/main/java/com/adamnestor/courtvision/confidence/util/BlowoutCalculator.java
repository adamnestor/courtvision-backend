package com.adamnestor.courtvision.confidence.util;

import com.adamnestor.courtvision.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BlowoutCalculator {
    private static final Logger logger = LoggerFactory.getLogger(BlowoutCalculator.class);

    private static final int SCALE = 4;
    private static final int BLOWOUT_THRESHOLD = 20;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal HOME_ADVANTAGE = new BigDecimal("2.5");

    private BlowoutCalculator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Calculates team strength differential incorporating net ratings and pace
     */
    public static BigDecimal calculateTeamStrengthDifferential(
            BigDecimal homeNetRating,
            BigDecimal awayNetRating,
            BigDecimal homePace,
            BigDecimal awayPace) {

        // Calculate net rating differential
        BigDecimal netRatingDiff = homeNetRating.subtract(awayNetRating);

        // Calculate pace impact (faster pace can increase blowout probability)
        BigDecimal avgPace = homePace.add(awayPace)
                .divide(new BigDecimal("2"), SCALE, RoundingMode.HALF_UP);
        BigDecimal paceImpact = avgPace.subtract(new BigDecimal("100.00"))
                .multiply(new BigDecimal("0.05")); // 5% weight for pace

        // Add home court advantage
        return netRatingDiff
                .add(paceImpact)
                .add(HOME_ADVANTAGE)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Analyzes a game's final score to determine if it was a blowout
     */
    public static boolean wasBlowout(Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null) {
            return false;
        }
        return Math.abs(homeScore - awayScore) >= BLOWOUT_THRESHOLD;
    }

    /**
     * Converts strength differential to probability using logistic function
     */
    public static BigDecimal calculateBlowoutProbability(BigDecimal strengthDifferential) {
        // Use logistic function to convert differential to probability
        double expValue = Math.exp(-0.05 * strengthDifferential.doubleValue());
        double probability = 1.0 / (1.0 + expValue);

        return BigDecimal.valueOf(probability)
                .multiply(HUNDRED)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates matchup-based probability adjustment based on historical games
     */
    public static BigDecimal calculateHistoricalMatchupFactor(int blowoutGames, int totalGames) {
        if (totalGames == 0) {
            return BigDecimal.ONE;
        }

        BigDecimal blowoutRate = BigDecimal.valueOf(blowoutGames)
                .divide(BigDecimal.valueOf(totalGames), SCALE, RoundingMode.HALF_UP);

        // Adjust base probability by up to 20% based on historical matchups
        return BigDecimal.ONE.add(blowoutRate.multiply(new BigDecimal("0.2")));
    }
}