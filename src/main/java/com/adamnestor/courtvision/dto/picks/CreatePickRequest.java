package com.adamnestor.courtvision.dto.picks;

import com.adamnestor.courtvision.domain.StatCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePickRequest(
        @NotNull
        Long playerId,

        @NotNull
        StatCategory category,

        @NotNull
        @Positive
        Integer threshold,

        @NotNull
        Double hitRateAtPick,

        @NotNull
        Boolean isParlay
) {}