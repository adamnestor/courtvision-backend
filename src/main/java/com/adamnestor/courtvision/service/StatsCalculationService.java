package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service interface for calculating player statistics, hit rates, and averages.
 */
public interface StatsCalculationService {
    /**
     * Calculates comprehensive hit rate statistics for a player.
     *
     * @param player The player to calculate stats for
     * @param category The statistical category to check (POINTS, ASSISTS, REBOUNDS)
     * @param threshold The value to check against
     * @param timePeriod The time period to analyze
     * @return Map containing hit rate, average, game counts, and additional analysis
     */
    Map<String, Object> calculateHitRate(Players player, StatCategory category,
                                         Integer threshold, TimePeriod timePeriod);

    /**
     * Retrieves basic statistical averages for a player over a specified time period.
     *
     * @param player The player to calculate stats for
     * @param timePeriod The time period to analyze (L5, L10, L15, L20, SEASON)
     * @return Map containing averages for points, assists, and rebounds
     */
    Map<StatCategory, BigDecimal> getPlayerAverages(Players player, TimePeriod timePeriod);

    /**
     * Verifies if there is sufficient data available for the requested time period.
     *
     * @param player The player to check
     * @param timePeriod The time period to verify
     * @return true if sufficient data exists, false otherwise
     */
    boolean hasSufficientData(Players player, TimePeriod timePeriod);
}