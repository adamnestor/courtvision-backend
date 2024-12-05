package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.common.ApiResponse;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.service.StatsCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final StatsCalculationService statsService;

    public DashboardController(StatsCalculationService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<List<DashboardStatsRow>>> getDashboardStats(
            @RequestParam(defaultValue = "L10") TimePeriod timePeriod,
            @RequestParam(required = false, defaultValue = "ALL") StatCategory category,
            @RequestParam(required = false) Integer threshold,
            @RequestParam(defaultValue = "hitRate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        logger.info("Fetching dashboard stats - period: {}, category: {}, threshold: {}, sort: {} {}",
                timePeriod, category, threshold, sortBy, sortDirection);

        // If specific category selected but no threshold provided, use default
        if (category != StatCategory.ALL && threshold == null){
            threshold = category.getDefaultThreshold();
        }

        // If category is ALL, ignore threshold
        if (category == StatCategory.ALL) {
            threshold = null;
        }

        // Only validate threshold if category is not ALL
        if (category != StatCategory.ALL && threshold != null) {
            validateThreshold(category, threshold);
        }

        List<DashboardStatsRow> stats = statsService.getDashboardStats(
                timePeriod,
                category,
                threshold,
                sortBy,
                sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    private void validateThreshold(StatCategory category, Integer threshold) {
        List<Integer> validThresholds = category.getValidThresholds();
        if (!validThresholds.contains(threshold)) {
            throw new IllegalArgumentException(
                    "Invalid threshold " + threshold + " for category " + category
            );
        }
    }
}