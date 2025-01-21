package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.api.model.ApiGameStats;
import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import org.springframework.stereotype.Component;

@Component
public class StatsMapper {

    public GameStats toEntity(ApiGameStats apiStats, Games game, Players player) {
        if (apiStats == null) {
            return null;
        }

        GameStats stats = new GameStats();
        stats.setExternalId(apiStats.getId());
        stats.setGame(game);
        stats.setPlayer(player);
        stats.setMinutesPlayed(apiStats.getMin());
        stats.setPoints(apiStats.getPoints());
        stats.setAssists(apiStats.getAssists());
        stats.setRebounds(apiStats.getRebounds());
        stats.setSteals(apiStats.getSteals());
        stats.setBlocks(apiStats.getBlocks());
        stats.setTurnovers(apiStats.getTurnovers());
        stats.setFieldGoalsMade(apiStats.getFgm());
        stats.setFieldGoalsAttempted(apiStats.getFga());
        stats.setThreePointersMade(apiStats.getFg3m());
        stats.setThreePointersAttempted(apiStats.getFg3a());
        stats.setFreeThrowsMade(apiStats.getFtm());
        stats.setFreeThrowsAttempted(apiStats.getFta());

        return stats;
    }

    public void updateEntity(GameStats existingStats, ApiGameStats apiStats) {
        if (apiStats == null) {
            return;
        }

        existingStats.setMinutesPlayed(apiStats.getMin());
        existingStats.setPoints(apiStats.getPoints());
        existingStats.setAssists(apiStats.getAssists());
        existingStats.setRebounds(apiStats.getRebounds());
        existingStats.setSteals(apiStats.getSteals());
        existingStats.setBlocks(apiStats.getBlocks());
        existingStats.setTurnovers(apiStats.getTurnovers());
        existingStats.setFieldGoalsMade(apiStats.getFgm());
        existingStats.setFieldGoalsAttempted(apiStats.getFga());
        existingStats.setThreePointersMade(apiStats.getFg3m());
        existingStats.setThreePointersAttempted(apiStats.getFg3a());
        existingStats.setFreeThrowsMade(apiStats.getFtm());
        existingStats.setFreeThrowsAttempted(apiStats.getFta());
    }
} 