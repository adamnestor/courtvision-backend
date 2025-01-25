package com.adamnestor.courtvision.dto.response;

import com.adamnestor.courtvision.domain.StatCategory;
import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for dashboard statistics endpoint.
 */
public record DashboardStatsResponse(
    Long playerId,
    String playerName,
    String team,
    String opponent,
    boolean isAway,
    StatCategory category,
    Integer threshold,
    BigDecimal hitRate,
    Integer confidenceScore,
    Integer gamesPlayed,
    BigDecimal average,
    List<Integer> lastGames,
    String gameTime
) {
} 