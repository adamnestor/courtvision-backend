package com.adamnestor.courtvision.dto.dashboard;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import java.math.BigDecimal;

public record DashboardStatsRow(
        Long playerId,
        String playerName,
        String teamAbbreviation,
        StatCategory category,
        Integer threshold,
        TimePeriod timePeriod,
        BigDecimal hitRate,
        BigDecimal average,
        Integer gamesAnalyzed
) {}