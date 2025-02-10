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
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.service.util.DateUtils;

@Service
public class DashboardService {
    private final PlayerPerformanceService hitRateCalculationService;
    private final DashboardMapper dashboardMapper;
    private final GamesRepository gamesRepository;
    private final DateUtils dateUtils;
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    public DashboardService(
            PlayerPerformanceService hitRateCalculationService,
            DashboardMapper dashboardMapper,
            GamesRepository gamesRepository,
            DateUtils dateUtils) {
        this.hitRateCalculationService = hitRateCalculationService;
        this.dashboardMapper = dashboardMapper;
        this.gamesRepository = gamesRepository;
        this.dateUtils = dateUtils;
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

        // Get actual count of today's games
        int totalGames = gamesRepository.findByGameDateAndStatus(
            dateUtils.getCurrentEasternDate(), 
            "scheduled"
        ).size();
        
        logger.debug("Total games found: {}", totalGames);

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
                if (sortBy == null || sortBy.equalsIgnoreCase("hitrate")) {
                    // First compare hit rates
                    int hitRateComparison = multiplier * compareNullSafe(b.hitRate(), a.hitRate());
                    // If hit rates are equal, compare confidence scores
                    if (hitRateComparison == 0) {
                        return multiplier * compareNullSafe(b.confidenceScore(), a.confidenceScore());
                    }
                    return hitRateComparison;
                }
                // Other cases remain the same
                return multiplier * switch (sortBy.toLowerCase()) {
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