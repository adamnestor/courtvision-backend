package com.adamnestor.courtvision.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import com.adamnestor.courtvision.mapper.DashboardMapper;
import com.adamnestor.courtvision.dto.response.DashboardMetadata;
import com.adamnestor.courtvision.dto.response.DashboardResponse;

@Service
public class DashboardService {
    private final HitRateCalculationService hitRateCalculationService;
    private final DashboardMapper dashboardMapper;
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    public DashboardService(
            HitRateCalculationService hitRateCalculationService,
            DashboardMapper dashboardMapper) {
        this.hitRateCalculationService = hitRateCalculationService;
        this.dashboardMapper = dashboardMapper;
    }

    public DashboardResponse getDashboardStats(
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

        // Filter out entries with null values
        stats = stats.stream()
            .filter(stat -> stat.hitRate() != null && stat.confidenceScore() != null)
            .collect(Collectors.toList());

        // Apply sorting
        if (sortBy != null && sortDir != null) {
            stats = sortDashboardStats(stats, sortBy, sortDir);
        }

        // Calculate metadata
        long uniqueTeams = stats.stream()
            .map(DashboardStatsResponse::team)
            .distinct()
            .count();
        
        int totalGames = (int) (uniqueTeams / 2);
        
        logger.debug("Unique teams found: {}, Total games calculated: {}", uniqueTeams, totalGames);

        DashboardMetadata metadata = new DashboardMetadata(
            totalGames,
            stats.size()  // total players with valid stats
        );

        return new DashboardResponse(stats, metadata);
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
                    return multiplier * compareNullSafe(b.confidenceScore(), a.confidenceScore());
                }
                return multiplier * switch (sortBy.toLowerCase()) {
                    case "hitrate" -> compareNullSafe(b.hitRate(), a.hitRate());
                    case "confidencescore" -> compareNullSafe(b.confidenceScore(), a.confidenceScore());
                    default -> compareNullSafe(b.confidenceScore(), a.confidenceScore());
                };
            })
            .collect(Collectors.toList());
    }

    private <T extends Comparable<T>> int compareNullSafe(T a, T b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }
} 