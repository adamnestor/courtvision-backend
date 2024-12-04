package com.adamnestor.courtvision.dto.dashboard;

import java.math.BigDecimal;

public record DashboardStatsRow(
        Long playerId,
        String playerName,
        String team,
        String opponent,
        String statLine,
        BigDecimal hitRate,
        BigDecimal average
) {}