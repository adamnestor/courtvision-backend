package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for calculating player statistics, hit rates, and averages.
 */
public interface HitRateCalculationService {
    /**
     * Calculates hit rate statistics for a player.
     *
     * @param player The player to calculate stats for
     * @param category The statistical category to check (POINTS, ASSISTS, REBOUNDS)
     * @param threshold The value to check against
     * @param timePeriod The time period to analyze
     * @return The calculated hit rate as a BigDecimal
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

    /**
     * Retrieves dashboard statistics for all active players with filtering and sorting.
     *
     * @param timePeriod The time period to analyze (L5, L10, L15, L20, SEASON)
     * @param category The statistical category to analyze (POINTS, ASSISTS, REBOUNDS)
     * @param threshold The value to check against for hit rates
     * @param sortBy The field to sort by (hitRate, average, gamesAnalyzed)
     * @param sortDirection The sort direction (asc, desc)
     * @return List of dashboard stats rows containing player hit rates and averages
     */
    List<DashboardStatsRow> getDashboardStats(TimePeriod timePeriod,
                                              StatCategory category,
                                              Integer threshold,
                                              String sortBy,
                                              String sortDirection);

    /**
     * Retrieves detailed player statistics including game-by-game performance.
     *
     * @param playerId The ID of the player to analyze
     * @param timePeriod The time period to analyze (L5, L10, L15, L20, SEASON)
     * @param category The statistical category to analyze (POINTS, ASSISTS, REBOUNDS)
     * @param threshold The value to check against for hit rates
     * @return PlayerDetailStats containing player info, game performances, and summary statistics
     */
    PlayerDetailStats getPlayerDetailStats(Long playerId,
                                           TimePeriod timePeriod,
                                           StatCategory category,
                                           Integer threshold);
}