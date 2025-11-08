package com.adamnestor.courtvision.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PlayerStatsResponse(
    Long playerId,
    String playerName,
    String team,
    BigDecimal hitRate,
    Integer gamesPlayed,
    BigDecimal average,
    List<GameStatDetail> recentGames
) {} 