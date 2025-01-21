package com.adamnestor.courtvision.dto.picks;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.StatCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for user pick data.
 */
public record UserPickDTO(
    Long id,
    Long playerId,
    String playerName,
    String team,
    String opponent,
    StatCategory category,
    Integer threshold,
    BigDecimal hitRateAtPick,
    Integer confidenceScore,
    Boolean result,
    LocalDate createdAt,
    Games game
) {}