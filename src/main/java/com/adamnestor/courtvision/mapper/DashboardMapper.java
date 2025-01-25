package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class DashboardMapper {

    @SuppressWarnings("unchecked")
    public DashboardStatsRow toStatsRow(Players player, Map<String, Object> stats,
                                        StatCategory category, Integer threshold,
                                        String opponent, boolean isAway) {
        List<Integer> lastGames = stats.get("lastGames") != null 
            ? (List<Integer>) stats.get("lastGames") 
            : List.of();

        return new DashboardStatsRow(
                player.getId(),
                player.getFirstName() + " " + player.getLastName(),
                player.getTeam().getAbbreviation(),
                opponent,
                isAway,
                category,
                threshold,
                (BigDecimal) stats.get("hitRate"),
                (Integer) stats.get("confidenceScore"),
                (Integer) stats.get("gamesPlayed"),
                (BigDecimal) stats.get("average"),
                lastGames
        );
    }
}