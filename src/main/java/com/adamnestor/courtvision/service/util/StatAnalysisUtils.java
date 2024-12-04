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
        logger.debug("Analyzing threshold {} for {} with {} games", threshold, category,
                games != null ? games.size() : "null");

        if (games == null || games.isEmpty()) {
            logger.warn("No games provided for analysis");
            return createEmptyAnalysis(category, threshold);
        }

        Map<String, Object> analysis = new HashMap<>();

        int successes = countSuccesses(games, category, threshold);
        logger.debug("Counted {} successes", successes);  // Debug log

        BigDecimal hitRate = calculateHitRate(games, category, threshold);
        logger.debug("Calculated hit rate: {}", hitRate);  // Debug log

        BigDecimal average = calculateAverage(games, category);
        logger.debug("Calculated average: {}", average);  // Debug log

        analysis.put("threshold", threshold);
        analysis.put("hitRate", hitRate);
        analysis.put("successCount", successes);
        analysis.put("failureCount", games.size() - successes);
        analysis.put("average", average);
        analysis.put("category", category);

        logger.debug("Final analysis: {}", analysis);  // Debug log
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
     * Returns standard thresholds for a category
     */
    public static List<Integer> getThresholdsForCategory(StatCategory category) {
        return switch (category) {
            case POINTS -> POINTS_THRESHOLDS;
            case ASSISTS -> ASSISTS_THRESHOLDS;
            case REBOUNDS -> REBOUNDS_THRESHOLDS;
            case ALL -> Collections.emptyList();
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
            case ALL -> throw new IllegalArgumentException("Cannot get stat value for category ALL");
        };
    }

    /**
     * Counts games meeting or exceeding the threshold
     */
    private static int countSuccesses(List<GameStats> games, StatCategory category, int threshold) {
        if (games == null) {
            logger.debug("Games list is null"); // Debug log
            return 0;
        }

        int count = (int) games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count();
        logger.debug("Count successes result: {}", count); // Debug log
        return count;
    }

    private static Map<String, Object> createEmptyAnalysis(StatCategory category, Integer threshold) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("threshold", threshold);
        analysis.put("hitRate", BigDecimal.ZERO);
        analysis.put("successCount", 0);
        analysis.put("failureCount", 0);
        analysis.put("average", BigDecimal.ZERO);
        analysis.put("category", category);
        return analysis;
    }
}