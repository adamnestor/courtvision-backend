package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;

import java.util.List;

public interface StatsService {
    /**
     * Retrieves and updates stats for a specific game
     *
     * @param game The game to fetch stats for
     * @return List of updated game stats
     */
    List<GameStats> getAndUpdateGameStats(Games game);

    /**
     * Retrieves and updates player's stats for a season
     *
     * @param player The player to fetch stats for
     * @param season The season year (e.g., 2024)
     * @return List of updated game stats
     */
    List<GameStats> getAndUpdatePlayerSeasonStats(Players player, Integer season);

    /**
     * Gets recent game stats for a player
     *
     * @param player The player to get stats for
     * @param limit Maximum number of games to return
     * @return List of recent game stats
     */
    List<GameStats> getRecentPlayerStats(Players player, int limit);

    /**
     * Gets all game stats for a specific game
     *
     * @param game The game to get stats for
     * @return List of game stats
     */
    List<GameStats> getGameStats(Games game);

    List<GameStats> getFilteredPlayerStats(
        Players player,
        int numGames,
        StatCategory category,
        Integer threshold
    );
} 