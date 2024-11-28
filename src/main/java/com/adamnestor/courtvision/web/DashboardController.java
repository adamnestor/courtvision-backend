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
            @RequestParam(required = false) StatCategory category,
            @RequestParam(required = false) Integer threshold,
            @RequestParam(defaultValue = "hitRate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        logger.info("Fetching dashboard stats - period: {}, category: {}, threshold: {}, sort: {} {}",
                timePeriod, category, threshold, sortBy, sortDirection);

        // Set default category if not provided
        if (category == null) {
            category = StatCategory.POINTS;
        }

        // Set default threshold if not provided
        if (threshold == null) {
            threshold = switch (category) {
                case POINTS -> 20;
                case ASSISTS -> 4;
                case REBOUNDS -> 8;
            };
        }

        List<DashboardStatsRow> stats = statsService.getDashboardStats(
                timePeriod, category, threshold, sortBy, sortDirection);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}