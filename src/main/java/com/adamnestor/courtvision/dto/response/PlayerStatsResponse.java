package com.adamnestor.courtvision.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO for player statistics endpoint.
 */
public record PlayerStatsResponse(
    Long playerId,
    String playerName,
    String team,
    BigDecimal hitRate,
    Integer confidenceScore,
    Integer gamesPlayed,
    BigDecimal average,
    boolean isHighConfidence
) {} 