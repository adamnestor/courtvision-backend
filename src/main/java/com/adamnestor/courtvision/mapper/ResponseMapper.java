package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import com.adamnestor.courtvision.dto.response.PlayerStatsResponse;
import org.springframework.stereotype.Component;

/**
 * Maps internal DTOs to standardized response DTOs.
 */
@Component
public class ResponseMapper {
    public PlayerStatsResponse toPlayerStatsResponse(PlayerDetailStats stats) {
        return new PlayerStatsResponse(
            stats.playerId(),
            stats.playerName(),
            stats.team(),
            stats.hitRate(),
            stats.confidenceScore(),
            stats.gamesPlayed(),
            stats.average(),
            stats.confidenceScore() >= 80
        );
    }

    public DashboardStatsResponse toDashboardResponse(DashboardStatsRow stats) {
        return new DashboardStatsResponse(
            stats.playerId(),
            stats.playerName(),
            stats.team(),
            stats.opponent(),
            stats.isAway(),
            stats.category().name(),
            stats.threshold(),
            stats.hitRate(),
            stats.confidenceScore(),
            stats.confidenceScore() >= 80
        );
    }
} 