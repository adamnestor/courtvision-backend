package com.adamnestor.courtvision.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for dashboard statistics endpoint.
 */
public record DashboardStatsResponse(
    Long playerId,
    String playerName,
    String team,
    String category,
    BigDecimal hitRate,
    Integer confidenceScore,
    Integer gamesPlayed,
    BigDecimal average,
    List<Integer> lastGames,
    boolean isHighConfidence
) {} 