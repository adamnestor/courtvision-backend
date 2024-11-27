package com.adamnestor.courtvision.service.util;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.StatCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Utility class for analyzing player statistics, including hit rates and threshold calculations.
 */
public class StatAnalysisUtils {
    private static final Logger logger = LoggerFactory.getLogger(StatAnalysisUtils.class);

    // Category-specific thresholds
    private static final List<Integer> POINTS_THRESHOLDS = Arrays.asList(10, 15, 20, 25);
    private static final List<Integer> ASSISTS_THRESHOLDS = Arrays.asList(2, 4, 6, 8);
    private static final List<Integer> REBOUNDS_THRESHOLDS = Arrays.asList(4, 6, 8, 10);
    private static final int DECIMAL_PLACES = 2;

    private StatAnalysisUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Analyzes hit rates and averages for all standard thresholds in a category
     */
    public static Map<String, Object> analyzeCategoryStats(List<GameStats> games, StatCategory category) {
        logger.debug("Analyzing all stats for category: {} with {} games", category, games.size());

        Map<String, Object> analysis = new HashMap<>();

        // Basic stats
        analysis.put("totalGames", games.size());
        analysis.put("average", calculateAverage(games, category));
        analysis.put("category", category);

        // Hit rates for all thresholds
        Map<Integer, Map<String, Object>> thresholdAnalysis = new TreeMap<>();
        for (Integer threshold : getThresholdsForCategory(category)) {
            thresholdAnalysis.put(threshold, analyzeThreshold(games, category, threshold));
        }
        analysis.put("thresholdAnalysis", thresholdAnalysis);

        return analysis;
    }

    /**
     * Analyzes performance for a specific threshold
     */
    public static Map<String, Object> analyzeThreshold(List<GameStats> games, StatCategory category, Integer threshold) {
        logger.debug("Analyzing threshold {} for {} with {} games", threshold, category, games.size());

        Map<String, Object> analysis = new HashMap<>();

        int successes = countSuccesses(games, category, threshold);
        BigDecimal hitRate = calculateHitRate(games, category, threshold);
        BigDecimal average = calculateAverage(games, category);

        analysis.put("threshold", threshold);
        analysis.put("hitRate", hitRate);
        analysis.put("successCount", successes);
        analysis.put("failureCount", games.size() - successes);
        analysis.put("average", average);
        analysis.put("consistency", calculateConsistencyScore(games, category, threshold));

        return analysis;
    }

    /**
     * Calculates the hit rate for a specific threshold
     */
    public static BigDecimal calculateHitRate(List<GameStats> games, StatCategory category, int threshold) {
        logger.debug("Calculating hit rate for {} threshold {}", category, threshold);

        if (games == null || games.isEmpty()) {
            logger.debug("No games provided for hit rate calculation");
            return BigDecimal.ZERO;
        }

        int successes = countSuccesses(games, category, threshold);
        return BigDecimal.valueOf(successes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(games.size()), DECIMAL_PLACES, RoundingMode.HALF_UP);
    }

    /**
     * Calculates average for a specific stat category
     */
    public static BigDecimal calculateAverage(List<GameStats> games, StatCategory category) {
        logger.debug("Calculating average for {} with {} games", category, games.size());

        if (games == null || games.isEmpty()) {
            logger.debug("No games provided for average calculation");
            return BigDecimal.ZERO;
        }

        double sum = games.stream()
                .mapToInt(game -> getStatValue(game, category))
                .sum();

        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(games.size()), DECIMAL_PLACES, RoundingMode.HALF_UP);
    }

    /**
     * Calculates a consistency score based on performance variance
     */
    private static BigDecimal calculateConsistencyScore(List<GameStats> games, StatCategory category, int threshold) {
        if (games == null || games.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double mean = games.stream()
                .mapToInt(game -> getStatValue(game, category))
                .average()
                .orElse(0.0);

        double variance = games.stream()
                .mapToDouble(game -> getStatValue(game, category))
                .map(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);

        // Convert variance to a 0-100 consistency score (lower variance = higher consistency)
        double maxVariance = Math.pow(threshold, 2);
        double consistencyScore = 100 * (1 - Math.min(variance / maxVariance, 1));

        return BigDecimal.valueOf(consistencyScore)
                .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);
    }

    /**
     * Returns standard thresholds for a category
     */
    public static List<Integer> getThresholdsForCategory(StatCategory category) {
        return switch (category) {
            case POINTS -> POINTS_THRESHOLDS;
            case ASSISTS -> ASSISTS_THRESHOLDS;
            case REBOUNDS -> REBOUNDS_THRESHOLDS;
        };
    }

    /**
     * Validates if a threshold is standard for a category
     */
    public static boolean isValidThreshold(StatCategory category, int threshold) {
        return getThresholdsForCategory(category).contains(threshold);
    }

    /**
     * Gets the stat value from a game based on category
     */
    private static int getStatValue(GameStats game, StatCategory category) {
        return switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
        };
    }

    /**
     * Counts games meeting or exceeding the threshold
     */
    private static int countSuccesses(List<GameStats> games, StatCategory category, int threshold) {
        return (int) games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count();
    }
}