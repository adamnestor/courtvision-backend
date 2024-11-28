package com.adamnestor.courtvision.dto.player;

public record PlayerInfo(
        Long playerId,
        String firstName,
        String lastName,
        String teamAbbreviation,
        String position
) {}