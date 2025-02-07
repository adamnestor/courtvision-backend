package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.api.model.ApiAdvancedStats;
import com.adamnestor.courtvision.domain.AdvancedGameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import org.springframework.stereotype.Component;

@Component
public class AdvancedStatsMapper {

    public AdvancedGameStats toEntity(ApiAdvancedStats apiStats, Games game, Players player) {
        if (apiStats == null) {
            return null;
        }

        AdvancedGameStats stats = new AdvancedGameStats();
        stats.setPlayer(player);
        stats.setGame(game);
        stats.setPie(apiStats.getPie());
        stats.setPace(apiStats.getPace() != null ? apiStats.getPace().doubleValue() : null);
        stats.setAssistPercentage(apiStats.getAssistPercentage());
        stats.setAssistRatio(apiStats.getAssistRatio());
        stats.setAssistToTurnover(apiStats.getAssistToTurnover());
        stats.setOffensiveRating(apiStats.getOffensiveRating());
        stats.setDefensiveRating(apiStats.getDefensiveRating());
        stats.setNetRating(apiStats.getNetRating());

        return stats;
    }

    public void updateEntity(AdvancedGameStats existingStats, ApiAdvancedStats apiStats) {
        if (apiStats == null) {
            return;
        }

        existingStats.setPie(apiStats.getPie());
        existingStats.setPace(apiStats.getPace() != null ? apiStats.getPace().doubleValue() : null);
        existingStats.setAssistPercentage(apiStats.getAssistPercentage());
        existingStats.setAssistRatio(apiStats.getAssistRatio());
        existingStats.setAssistToTurnover(apiStats.getAssistToTurnover());
        existingStats.setOffensiveRating(apiStats.getOffensiveRating());
        existingStats.setDefensiveRating(apiStats.getDefensiveRating());
        existingStats.setNetRating(apiStats.getNetRating());
    }
} 