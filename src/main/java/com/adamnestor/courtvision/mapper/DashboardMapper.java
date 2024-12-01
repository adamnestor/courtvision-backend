package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class DashboardMapper {

    public DashboardStatsRow toStatsRow(Players player, Map<String, Object> stats,
                                        StatCategory category, TimePeriod timePeriod) {
        return new DashboardStatsRow(
                player.getId(),
                player.getFirstName() + " " + player.getLastName(),
                player.getTeam().getAbbreviation(),
                category,
                (Integer) stats.get("threshold"),
                timePeriod,
                (BigDecimal) stats.get("hitRate"),
                (BigDecimal) stats.get("average"),
                (Integer) stats.get("successCount") + (Integer) stats.get("failureCount")
        );
    }
}