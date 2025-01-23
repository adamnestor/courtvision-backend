package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.Games;
import java.time.LocalDate;
import java.util.List;

public interface GameService {
    /**
     * Retrieves games for a specific date and updates the database
     *
     * @param date The date to fetch games for
     * @return List of updated game entities
     */
    List<Games> getAndUpdateGames(LocalDate date);

    /**
     * Retrieves games for a specific season and updates the database
     *
     * @param season The season year (e.g., 2024)
     * @return List of updated game entities
     */
    List<Games> getAndUpdateGamesBySeason(Integer season);

    /**
     * Retrieves games for a specific year and month and updates the database
     *
     * @param year The year to fetch games for
     * @param month The month to fetch games for
     * @return List of updated game entities
     */
    List<Games> getAndUpdateGamesByYearMonth(int year, int month);

    /**
     * Retrieves a specific game by its external ID
     *
     * @param externalId The BallDontLie API game ID
     * @return The game entity if found
     */
    Games findByExternalId(Long externalId);

    /**
     * Gets all games scheduled for today
     *
     * @return List of today's games
     */
    List<Games> getTodaysGames();

    /**
     * Gets all games for a date range
     *
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return List of games within the date range
     */
    List<Games> getGamesByDateRange(LocalDate startDate, LocalDate endDate);
} 