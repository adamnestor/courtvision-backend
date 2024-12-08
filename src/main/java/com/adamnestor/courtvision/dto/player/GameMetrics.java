package com.adamnestor.courtvision.dto.player;

public record GameMetrics(
   Integer maxValue,
   Integer minValue,
   Double averageValue,
   Integer totalGames,
   Integer gamesAboveThreshold
) {}
