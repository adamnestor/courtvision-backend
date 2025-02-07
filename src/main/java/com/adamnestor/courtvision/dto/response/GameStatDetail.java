package com.adamnestor.courtvision.dto.response;

/**
 * Response DTO for player statistics endpoint.
 */

public record GameStatDetail(
    String gameDate,
    String opponent,
    boolean isAway,
    int statValue,
    boolean hitThreshold
) {} 