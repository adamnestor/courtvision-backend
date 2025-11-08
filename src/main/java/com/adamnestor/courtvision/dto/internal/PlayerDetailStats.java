package com.adamnestor.courtvision.dto.internal;

import java.math.BigDecimal;
import java.util.List;
import com.adamnestor.courtvision.dto.response.GameStatDetail;

/**
 * DTO containing detailed player statistics.
 */
public record PlayerDetailStats(
    Long playerId,
    String playerName,
    String team,
    BigDecimal hitRate,
    Integer gamesPlayed,
    BigDecimal average,
    List<GameStatDetail> recentGames
) {}