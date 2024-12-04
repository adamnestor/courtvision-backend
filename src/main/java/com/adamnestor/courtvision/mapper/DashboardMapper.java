package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;

@Component
public class DashboardMapper {

    public DashboardStatsRow toStatsRow(Players player, Map<String, Object> stats,
                                        StatCategory category, Integer threshold,
                                        String opponent) {
        return new DashboardStatsRow(
                player.getId(),
                player.getFirstName() + " " + player.getLastName(),
                player.getTeam().getAbbreviation(),
                opponent,
                formatStatLine(category, threshold),
                (BigDecimal) stats.get("hitRate"),
                (BigDecimal) stats.get("average")
        );
    }

    private String formatStatLine(StatCategory category, Integer threshold) {
        if (category == StatCategory.ALL || threshold == null) {
            return "";
        }
        return category.name().charAt(0) +
                category.name().substring(1).toLowerCase() +
                " " + threshold + "+";
    }
}