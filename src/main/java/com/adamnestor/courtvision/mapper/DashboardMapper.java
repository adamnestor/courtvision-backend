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

    public DashboardStatsRow toStatsRow(Players player, Map<String, Object> stats,
                                        StatCategory category, Integer threshold,
                                        String opponent) {
        return new DashboardStatsRow(
                player.getId(),
                player.getFirstName() + " " + player.getLastName(),
                player.getTeam().getAbbreviation(),
                category,
                (BigDecimal) stats.get("hitRate"),
                (Integer) stats.get("confidenceScore"),
                (Integer) stats.get("successCount"),
                (BigDecimal) stats.get("average"),
                List.of()
        );
    }
}