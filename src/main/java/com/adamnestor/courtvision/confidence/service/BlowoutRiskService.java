package com.adamnestor.courtvision.confidence.service;

import com.adamnestor.courtvision.confidence.model.BlowoutImpact;
import com.adamnestor.courtvision.domain.*;
import java.math.BigDecimal;

/**
 * Service interface for calculating and analyzing blowout risks
 */
public interface BlowoutRiskService {

    /**
     * Calculates the overall blowout risk for a game
     *
     * @param game The game to analyze
     * @param player The player to analyze
     * @return Risk score from 0-100
     */
    BigDecimal calculateBlowoutRisk(Games game, Players player);

    /**
     * Analyzes the historical impact of blowouts on a player's performance
     *
     * @param player The player to analyze
     * @return BlowoutImpact containing retention factors
     */
    BlowoutImpact analyzePlayerBlowoutImpact(Players player);

    /**
     * Determines if a game is at high risk of being a blowout
     *
     * @param game The game to analyze
     * @return true if blowout risk is significant
     */
    boolean isHighBlowoutRisk(Games game);
}