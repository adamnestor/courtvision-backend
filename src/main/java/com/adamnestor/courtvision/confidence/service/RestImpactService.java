package com.adamnestor.courtvision.confidence.service;

import com.adamnestor.courtvision.confidence.model.RestImpact;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for calculating and analyzing rest impact on player performance.
 */
public interface RestImpactService {

    /**
     * Calculates the rest impact for a player's upcoming game.
     *
     * @param player the player to analyze
     * @param game the upcoming game
     * @param category the statistical category being analyzed
     * @return RestImpact containing rest days and impact calculations
     */
    RestImpact calculateRestImpact(Players player, Games game, StatCategory category);

    /**
     * Gets the historical performance metrics for a player based on rest days.
     *
     * @param player the player to analyze
     * @param daysOfRest number of rest days to analyze
     * @param category the statistical category
     * @return average performance metric for the specified rest scenario
     */
    BigDecimal getHistoricalRestPerformance(Players player, int daysOfRest, StatCategory category);

    /**
     * Analyzes a player's recent games to determine rest pattern impact.
     *
     * @param player the player to analyze
     * @param category the statistical category
     * @return list of rest impacts for recent games
     */
    List<RestImpact> analyzeRecentRestPattern(Players player, StatCategory category);

    /**
     * Determines if a game is part of a back-to-back set.
     *
     * @param game the game to check
     * @param player the player to check
     * @return true if the game is part of a back-to-back set
     */
    boolean isBackToBack(Games game, Players player);
}