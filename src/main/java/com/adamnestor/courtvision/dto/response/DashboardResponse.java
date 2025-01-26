package com.adamnestor.courtvision.dto.response;

import java.util.List;

public record DashboardResponse(
    List<DashboardStatsResponse> stats,
    DashboardMetadata metadata
) {} 