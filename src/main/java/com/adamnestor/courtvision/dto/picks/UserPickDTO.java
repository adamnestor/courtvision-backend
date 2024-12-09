package com.adamnestor.courtvision.dto.picks;

import com.adamnestor.courtvision.domain.StatCategory;
import java.time.LocalDateTime;

public record UserPickDTO(
        Long id,
        Long playerId,
        String playerName,
        String team,
        String opponent,
        StatCategory category,
        Integer threshold,
        Double hitRateAtPick,
        Boolean result,
        LocalDateTime createdAt
) {}