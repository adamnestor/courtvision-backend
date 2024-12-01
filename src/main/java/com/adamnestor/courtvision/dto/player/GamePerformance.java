package com.adamnestor.courtvision.dto.player;

import java.time.LocalDate;

public record GamePerformance(
        Long gameId,
        LocalDate gameDate,
        String opponent,
        boolean isHome,
        Integer points,
        Integer assists,
        Integer rebounds,
        String minutesPlayed,
        String score
) {}