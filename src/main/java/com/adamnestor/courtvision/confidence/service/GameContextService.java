package com.adamnestor.courtvision.confidence.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.confidence.model.GameContext;

public interface GameContextService {

    /**
     * Calculates all game context factors and returns a comprehensive result
     *
     * @param player Player to analyze
     * @param game Game to analyze
     * @param category Statistical category
     * @param threshold Statistical threshold
     * @return GameContext containing all impact factors and overall score
     */
    GameContext calculateGameContext(Players player, Games game, StatCategory category, Integer threshold);

    /**
     * Analyzes if the game context is favorable for the specified threshold
     *
     * @param player Player to analyze
     * @param game Game to analyze
     * @param category Statistical category
     * @param threshold Statistical threshold
     * @return true if context is favorable, false otherwise
     */
    boolean isFavorableContext(Players player, Games game, StatCategory category, Integer threshold);
}