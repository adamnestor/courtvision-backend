package com.adamnestor.courtvision.dto.response;

import java.math.BigDecimal;
import com.adamnestor.courtvision.domain.*;

/**
 * Response DTO for dashboard statistics endpoint.
 */
public record DashboardStatsResponse(
    Long playerId,
    String playerName,
    String team,
    String opponent,
    boolean isAway,
    String category,      // "POINTS", "ASSISTS", "REBOUNDS"
    Integer threshold,    // e.g., 15, 20, 25
    BigDecimal hitRate,   // percentage
    Integer confidenceScore, // percentage
    boolean isHighConfidence // true if confidenceScore >= 80
) {
    public static DashboardStatsResponse create(
            Players player,
            Games game,
            StatCategory category,
            Integer threshold,
            BigDecimal hitRate,
            Integer confidenceScore) {
        boolean isHome = game.getHomeTeam().equals(player.getTeam());
        return new DashboardStatsResponse(
            player.getId(),
            player.getFirstName() + " " + player.getLastName(),
            player.getTeam().getAbbreviation(),
            isHome ? game.getAwayTeam().getAbbreviation() : game.getHomeTeam().getAbbreviation(),
            !isHome,
            category.name(),
            threshold,
            hitRate,
            confidenceScore,
            confidenceScore >= 80
        );
    }
} 