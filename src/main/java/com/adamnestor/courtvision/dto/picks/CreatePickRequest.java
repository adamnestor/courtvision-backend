package com.adamnestor.courtvision.dto.picks;

import com.adamnestor.courtvision.domain.StatCategory;
import jakarta.validation.constraints.NotNull;

public record CreatePickRequest(
        @NotNull Long playerId,
        @NotNull StatCategory category,
        @NotNull Integer threshold,
        @NotNull Double hitRateAtPick,
        @NotNull Boolean isParlay
) {}