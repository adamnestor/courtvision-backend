package com.adamnestor.courtvision.confidence.util;

import com.adamnestor.courtvision.confidence.model.BlowoutImpact;
import com.adamnestor.courtvision.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class BlowoutCalculator {
    private static final Logger logger = LoggerFactory.getLogger(BlowoutCalculator.class);

    private static final int SCALE = 4;
    private static final int BLOWOUT_THRESHOLD = 20;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal LEAGUE_AVG_PIE = new BigDecimal("0.100");
    private static final BigDecimal LEAGUE_AVG_USAGE = new BigDecimal("20.00");
    private static final BigDecimal HOME_ADVANTAGE = new BigDecimal("3.5");

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

        // Calculate pace impact
        BigDecimal paceDiff = homePace.subtract(awayPace);
        BigDecimal paceImpact = paceDiff.multiply(new BigDecimal("0.1"));

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
     * Calculates player's performance retention based on PIE and usage rate
     */
    public static BigDecimal calculatePerformanceRetention(
            BigDecimal pie,
            BigDecimal usagePercentage) {

        if (pie == null || usagePercentage == null) {
            return BigDecimal.ONE;
        }

        // PIE impact (60% weight)
        BigDecimal pieImpact = pie
                .divide(LEAGUE_AVG_PIE, SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("0.6"));

        // Usage impact (40% weight)
        BigDecimal usageImpact = usagePercentage
                .divide(LEAGUE_AVG_USAGE, SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("0.4"));

        return pieImpact.add(usageImpact)
                .min(BigDecimal.ONE)
                .max(new BigDecimal("0.5"))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Converts strength differential to probability using logistic function
     */
    public static BigDecimal calculateBlowoutProbability(BigDecimal strengthDifferential) {
        // Use logistic function to convert differential to probability
        double expValue = Math.exp(-0.1 * strengthDifferential.doubleValue());
        double probability = 1.0 / (1.0 + expValue);

        return BigDecimal.valueOf(probability)
                .multiply(HUNDRED)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates minutes retention factor based on historical blowout data
     */
    public static BigDecimal calculateMinutesRetention(
            List<GameStats> blowoutGames,
            List<GameStats> normalGames) {

        if (blowoutGames.isEmpty() || normalGames.isEmpty()) {
            return BigDecimal.ONE;
        }

        // Calculate average minutes in blowouts vs normal games
        double blowoutAvg = blowoutGames.stream()
                .mapToDouble(g -> parseMinutes(g.getMinutesPlayed()))
                .average()
                .orElse(0.0);

        double normalAvg = normalGames.stream()
                .mapToDouble(g -> parseMinutes(g.getMinutesPlayed()))
                .average()
                .orElse(0.0);

        if (normalAvg == 0.0) {
            return BigDecimal.ONE;
        }

        return BigDecimal.valueOf(blowoutAvg / normalAvg)
                .min(BigDecimal.ONE)
                .max(new BigDecimal("0.5"))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static double parseMinutes(String minutesPlayed) {
        if (minutesPlayed == null || minutesPlayed.isEmpty()) {
            return 0.0;
        }
        try {
            String[] parts = minutesPlayed.split(":");
            return Integer.parseInt(parts[0]) + (parts.length > 1 ? Integer.parseInt(parts[1]) / 60.0 : 0);
        } catch (Exception e) {
            logger.warn("Error parsing minutes: {}", minutesPlayed);
            return 0.0;
        }
    }
}