package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.player.GamePerformance;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.player.PlayerInfo;
import com.adamnestor.courtvision.dto.stats.StatsSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PlayerMapper {

    public PlayerDetailStats toPlayerDetailStats(Players player,
                                                 List<GameStats> games,
                                                 Map<String, Object> statsSummary,
                                                 StatCategory category,
                                                 TimePeriod timePeriod) {
        return new PlayerDetailStats(
                toPlayerInfo(player),
                toGamePerformances(games),
                toStatsSummary(statsSummary, category, timePeriod)
        );
    }

    public PlayerInfo toPlayerInfo(Players player) {
        return new PlayerInfo(
                player.getId(),
                player.getFirstName(),
                player.getLastName(),
                player.getTeam().getAbbreviation(),
                player.getPosition()
        );
    }

    private List<GamePerformance> toGamePerformances(List<GameStats> games) {
        return games.stream()
                .map(game -> new GamePerformance(
                        game.getGame().getId(),
                        game.getGame().getGameDate(),
                        determineOpponent(game),
                        isHomeGame(game),
                        game.getPoints(),
                        game.getAssists(),
                        game.getRebounds(),
                        game.getMinutesPlayed(),
                        formatScore(game.getGame())
                ))
                .collect(Collectors.toList());
    }

    private StatsSummary toStatsSummary(Map<String, Object> stats,
                                        StatCategory category,
                                        TimePeriod timePeriod) {
        return new StatsSummary(
                category,
                (Integer) stats.get("threshold"),
                timePeriod,
                (BigDecimal) stats.get("hitRate"),
                (BigDecimal) stats.get("average"),
                (Integer) stats.get("successCount"),
                (Integer) stats.get("failureCount")
        );
    }

    private String determineOpponent(GameStats gameStats) {
        Games game = gameStats.getGame();
        Players player = gameStats.getPlayer();
        Teams playerTeam = player.getTeam();

        return playerTeam.getId().equals(game.getHomeTeam().getId())
                ? game.getAwayTeam().getAbbreviation()
                : game.getHomeTeam().getAbbreviation();
    }

    private boolean isHomeGame(GameStats gameStats) {
        return gameStats.getPlayer().getTeam().getId()
                .equals(gameStats.getGame().getHomeTeam().getId());
    }

    private String formatScore(Games game) {
        return String.format("%d-%d",
                game.getHomeTeamScore(),
                game.getAwayTeamScore());
    }
}