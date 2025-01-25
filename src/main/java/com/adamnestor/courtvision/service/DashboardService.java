package com.adamnestor.courtvision.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import com.adamnestor.courtvision.mapper.DashboardMapper;

@Service
public class DashboardService {
    private final HitRateCalculationService hitRateCalculationService;
    private final DashboardMapper dashboardMapper;

    public DashboardService(
            HitRateCalculationService hitRateCalculationService,
            DashboardMapper dashboardMapper) {
        this.hitRateCalculationService = hitRateCalculationService;
        this.dashboardMapper = dashboardMapper;
    }

    public List<DashboardStatsResponse> getDashboardStats(
        String timeFrame,
        StatCategory category,
        Integer threshold,
        String sortBy,
        String sortDir
    ) {
        // Get stats and map directly to response
        List<DashboardStatsResponse> stats = hitRateCalculationService.calculateDashboardStats(
            timeFrame,
            category,
            threshold,
            dashboardMapper
        );

        // Apply sorting
        if (sortBy != null && sortDir != null) {
            stats = sortDashboardStats(stats, sortBy, sortDir);
        }

        return stats;
    }

    private List<DashboardStatsResponse> sortDashboardStats(
        List<DashboardStatsResponse> stats,
        String sortBy,
        String sortDir
    ) {
        int multiplier = sortDir.equalsIgnoreCase("desc") ? -1 : 1;
        
        return stats.stream()
            .sorted((a, b) -> {
                if (sortBy == null) {
                    return multiplier * b.confidenceScore().compareTo(a.confidenceScore());
                }
                return multiplier * switch (sortBy.toLowerCase()) {
                    case "hitrate" -> b.hitRate().compareTo(a.hitRate());
                    case "confidencescore" -> b.confidenceScore().compareTo(a.confidenceScore());
                    default -> b.confidenceScore().compareTo(a.confidenceScore());
                };
            })
            .collect(Collectors.toList());
    }
} 