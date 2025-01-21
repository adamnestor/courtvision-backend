package com.adamnestor.courtvision.dto.player;

import java.math.BigDecimal;

/**
 * DTO containing detailed player statistics.
 */
public record PlayerDetailStats(
    Long playerId,
    String playerName,
    String team,
    BigDecimal hitRate,
    Integer confidenceScore,
    Integer gamesPlayed,
    BigDecimal average
) {}