package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.AdvancedGameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;

import java.util.List;
import java.util.Optional;

public interface AdvancedStatsService {
    /**
     * Retrieves and updates advanced stats for a specific game
     *
     * @param game The game to fetch advanced stats for
     * @return List of updated advanced game stats
     */
    List<AdvancedGameStats> getAndUpdateGameAdvancedStats(Games game);

    /**
     * Retrieves and updates player's advanced stats for a season
     *
     * @param player The player to fetch stats for
     * @param season The season year (e.g., 2024)
     * @return List of updated advanced game stats
     */
    List<AdvancedGameStats> getAndUpdatePlayerSeasonAdvancedStats(Players player, Integer season);

    /**
     * Gets advanced stats for a specific game and player
     *
     * @param game The game to get stats for
     * @param player The player to get stats for
     * @return Optional containing advanced stats if found
     */
    Optional<AdvancedGameStats> getAdvancedStats(Games game, Players player);

    /**
     * Gets recent advanced stats for a player
     *
     * @param player The player to get stats for
     * @param limit Maximum number of games to return
     * @return List of recent advanced game stats
     */
    List<AdvancedGameStats> getRecentAdvancedStats(Players player, int limit);
} 