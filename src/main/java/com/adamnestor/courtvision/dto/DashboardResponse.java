package com.adamnestor.courtvision.dto;

import java.util.List;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;

public record DashboardResponse(
    List<DashboardStatsResponse> data,
    DashboardMetadata meta,
    PageInfo pagination
) {} 