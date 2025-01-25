package com.adamnestor.courtvision.dto.dashboard;

import com.adamnestor.courtvision.domain.StatCategory;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for dashboard statistics row data.
 */
public record DashboardStatsRow(
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
    List<Integer> lastGames
) {
    public Integer getConfidenceScore() {
        return confidenceScore != null ? confidenceScore : 0;
    }
}