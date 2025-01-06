package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import java.math.BigDecimal;

public interface ConfidenceScoreService {
    /**
     * Calculates the overall confidence score for a player hitting a specific threshold
     * in an upcoming game.
     *
     * @param player Player to calculate confidence score for
     * @param game Upcoming game
     * @param threshold Statistical threshold to evaluate
     * @param category Statistical category (POINTS, ASSISTS, REBOUNDS)
     * @return Confidence score as a percentage (0-100)
     */
    BigDecimal calculateConfidenceScore(Players player, Games game, Integer threshold, StatCategory category);

    /**
     * Calculates the recent performance component of the confidence score
     * using exponential decay weighting.
     *
     * @param player Player to evaluate
     * @param threshold Statistical threshold
     * @param category Statistical category
     * @return Recent performance score
     */
    BigDecimal calculateRecentPerformance(Players player, Integer threshold, StatCategory category);

    /**
     * Calculates the impact of advanced metrics on the confidence score.
     *
     * @param player Player to evaluate
     * @param game Upcoming game
     * @param category Statistical category
     * @return Advanced metrics impact score
     */
    BigDecimal calculateAdvancedImpact(Players player, Games game, StatCategory category);

    /**
     * Calculates the game context component of the confidence score.
     *
     * @param player Player to evaluate
     * @param game Upcoming game
     * @param category Statistical category
     * @return Game context score
     */
    BigDecimal calculateGameContext(Players player, Games game, StatCategory category);

    /**
     * Calculates the risk of a blowout affecting player performance.
     *
     * @param game Game to evaluate
     * @return Blowout risk score
     */
    BigDecimal calculateBlowoutRisk(Games game);
}