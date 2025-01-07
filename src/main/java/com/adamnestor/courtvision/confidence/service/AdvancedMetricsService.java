package com.adamnestor.courtvision.confidence.service;

import com.adamnestor.courtvision.domain.*;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Service interface for analyzing and calculating impacts of advanced metrics
 * using data from the BallDontLie API.
 */
public interface AdvancedMetricsService {

    /**
     * Calculates the overall impact of advanced metrics on performance probability
     *
     * @param player The player to analyze
     * @param game The game to analyze
     * @param category The statistical category (POINTS, ASSISTS, REBOUNDS)
     * @return Impact score from 0-100
     */
    BigDecimal calculateAdvancedImpact(Players player, Games game, StatCategory category);

    /**
     * Analyzes historical impact of Player Impact Estimate (PIE)
     *
     * @param player The player to analyze
     * @param threshold The statistical threshold
     * @param category The statistical category
     * @return Score indicating PIE's impact on threshold achievement
     */
    BigDecimal analyzePIEImpact(Players player, Integer threshold, StatCategory category);

    /**
     * Analyzes impact of usage rate on performance
     *
     * @param player The player to analyze
     * @param game The game to analyze
     * @param category The statistical category
     * @return Score indicating usage rate's impact
     */
    BigDecimal analyzeUsageRateImpact(Players player, Games game, StatCategory category);

    /**
     * Gets normalized weights for advanced metrics based on category
     *
     * @param category The statistical category
     * @return Map of metric names to their weights
     */
    Map<String, BigDecimal> getCategoryWeights(StatCategory category);

    /**
     * Retrieves latest advanced stats for a player
     *
     * @param player The player to analyze
     * @return The most recent AdvancedGameStats
     */
    AdvancedGameStats getLatestAdvancedStats(Players player);
}