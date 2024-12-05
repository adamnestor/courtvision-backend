package com.adamnestor.courtvision.dto.player;

import com.adamnestor.courtvision.dto.stats.StatsSummary;
import java.util.List;

public record PlayerDetailStats(
        PlayerInfo player,
        List<GamePerformance> games,
        StatsSummary summary,
        Integer threshold,
        GameMetrics metrics
) {}