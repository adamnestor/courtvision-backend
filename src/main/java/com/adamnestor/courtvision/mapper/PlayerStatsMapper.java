package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.dto.internal.PlayerDetailStats;
import com.adamnestor.courtvision.dto.internal.StatsSummary;
import org.springframework.stereotype.Component;

@Component
public class PlayerStatsMapper {
    public PlayerDetailStats toPlayerDetailStats(
        Players player,
        StatsSummary summary,
        Integer threshold
    ) {
        return new PlayerDetailStats(
            player.getId(),
            player.getFirstName() + " " + player.getLastName(),
            player.getTeam().getAbbreviation(),
            summary.hitRate(),
            summary.confidenceScore(),
            summary.successCount(),
            summary.average(),
            summary.recentGames()
        );
    }
} 