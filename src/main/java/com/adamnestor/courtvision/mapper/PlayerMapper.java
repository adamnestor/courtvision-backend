package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.player.*;
import com.adamnestor.courtvision.dto.stats.StatsSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PlayerMapper {

    public PlayerDetailStats toPlayerDetailStats(
            Players player,
            StatsSummary summary,
            Integer threshold) {
        
        return new PlayerDetailStats(
            player.getId(),
            player.getFirstName() + " " + player.getLastName(),
            player.getTeam().getAbbreviation(),
            summary.hitRate(),
            summary.confidenceScore(),
            summary.successCount(),
            summary.average()
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

    public List<GamePerformance> toGamePerformances(List<GameStats> games,
                                                  StatCategory category,
                                                  Integer threshold) {
        return games.stream()
                .map(game -> {
                    int selectedValue = getStatValue(game, category);
                    return new GamePerformance(
                        game.getGame().getId(),
                        game.getGame().getGameDate(),
                        determineOpponent(game),
                        isHomeGame(game),
                        game.getPoints(),
                        game.getAssists(),
                        game.getRebounds(),
                        String.valueOf(game.getMinutesPlayed()),
                        formatScore(game.getGame()),
                        selectedValue >= threshold,
                        Integer.valueOf(selectedValue)
                    );
                })
                .collect(Collectors.toList());
    }

    public StatsSummary toStatsSummary(Map<String, Object> stats,
                                      StatCategory category,
                                      TimePeriod timePeriod) {
        return new StatsSummary(
            category,
            (Integer) stats.get("threshold"),
            timePeriod,
            (BigDecimal) stats.get("hitRate"),
            (BigDecimal) stats.get("average"),
            (Integer) stats.get("successCount"),
            (Integer) stats.get("confidenceScore")
        );
    }

    public GameMetrics calculateGameMetrics(List<GameStats> games,
                                            StatCategory category,
                                            Integer threshold) {
        if (games.isEmpty()) {
            return new GameMetrics(0, 0, 0.0, 0, 0);
        }

        List<Integer> values = games.stream()
                .map(game -> getStatValue(game, category))
                .collect(Collectors.toList());

        return new GameMetrics(
                Collections.max(values),
                Collections.min(values),
                values.stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0),
                games.size(),
                (int) values.stream()
                        .filter(v -> v >= threshold)
                        .count()
        );
    }

    private int getStatValue(GameStats game, StatCategory category) {
        return switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
            default -> throw new IllegalArgumentException("Invalid category: " + category);
        };
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