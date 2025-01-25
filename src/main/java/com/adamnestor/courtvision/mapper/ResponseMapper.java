package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.dto.picks.UserPickDTO;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import com.adamnestor.courtvision.dto.response.PickResponse;
import com.adamnestor.courtvision.dto.response.PlayerStatsResponse;
import com.adamnestor.courtvision.util.ResponseUtils;
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

    public PickResponse toPickResponse(UserPickDTO pick) {
        return new PickResponse(
            pick.id(),
            pick.playerId(),
            pick.playerName(),
            pick.team(),
            pick.opponent(),
            pick.category().toString(),
            pick.threshold(),
            pick.hitRateAtPick(),
            pick.confidenceScore(),
            ResponseUtils.formatGameResult(pick.result()),
            ResponseUtils.formatDateTime(pick.createdAt()),
            pick.game().getGameTime()
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