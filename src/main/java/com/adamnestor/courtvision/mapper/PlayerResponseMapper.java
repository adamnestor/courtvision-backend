package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.response.PlayerStatsResponse;
import org.springframework.stereotype.Component;

@Component
public class PlayerResponseMapper {
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
} 