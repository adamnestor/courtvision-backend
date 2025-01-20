package com.adamnestor.courtvision.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO for user picks endpoint.
 */
public record PickResponse(
    Long id,
    Long playerId,
    String playerName,
    String team,
    String opponent,
    String category,
    Integer threshold,
    BigDecimal hitRateAtPick,
    Integer confidenceScore,
    String result,
    String createdAt,
    String gameTime
) {} 