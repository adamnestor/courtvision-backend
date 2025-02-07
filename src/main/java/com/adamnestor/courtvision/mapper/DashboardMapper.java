package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class DashboardMapper {

    @SuppressWarnings("unchecked")
    public DashboardStatsResponse toStatsResponse(
        Players player, 
        Games game, 
        Map<String, Object> stats,
        String opponent, 
        boolean isAway
    ) {
        List<Integer> lastGames = stats.get("lastGames") != null 
            ? (List<Integer>) stats.get("lastGames") 
            : List.of();

        return new DashboardStatsResponse(
            player.getId(),
            player.getFirstName() + " " + player.getLastName(),
            player.getTeam().getAbbreviation(),
            opponent,
            isAway,
            (StatCategory) stats.get("category"),
            (Integer) stats.get("threshold"),
            (BigDecimal) stats.get("hitRate"),
            (Integer) stats.get("confidenceScore"),
            (Integer) stats.get("gamesPlayed"),
            (BigDecimal) stats.get("average"),
            lastGames,
            game.getGameTime()
        );
    }
}