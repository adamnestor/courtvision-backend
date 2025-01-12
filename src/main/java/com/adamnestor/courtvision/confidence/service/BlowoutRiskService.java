package com.adamnestor.courtvision.confidence.service;

import com.adamnestor.courtvision.domain.*;
import java.math.BigDecimal;

/**
 * Service interface for calculating game-level blowout risks
 */
public interface BlowoutRiskService {

    /**
     * Calculates the probability of a blowout occurring in a game
     * based on team strength differentials, pace, and historical matchups
     *
     * @param game The game to analyze
     * @return Risk score from 0-100
     */
    BigDecimal calculateBlowoutRisk(Games game);

    /**
     * Determines if a game is at high risk of being a blowout
     *
     * @param game The game to analyze
     * @return true if blowout risk is above threshold
     */
    boolean isHighBlowoutRisk(Games game);
}