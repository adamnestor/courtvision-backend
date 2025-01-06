package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.common.ServiceResponse;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.service.HitRateCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "NBA Statistics Dashboard APIs")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final HitRateCalculationService statsService;

    public DashboardController(HitRateCalculationService statsService) {
        this.statsService = statsService;
    }

    @Operation(
            summary = "Get dashboard statistics",
            description = "Retrieves filtered and sorted statistics for display on the main dashboard. " +
                    "Supports filtering by time period, statistical category, and threshold values. " +
                    "Results can be sorted by hit rate or average value. When category is 'ALL', " +
                    "returns stats for all categories using their default thresholds." +
                    "\n\nFeatures:" +
                    "\n- Flexible time period selection (L5 to Season)" +
                    "\n- Category-specific threshold filtering" +
                    "\n- Customizable sorting options" +
                    "\n- Includes hit rates and averages" +
                    "\n- Supports multi-category overview"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved dashboard statistics",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = ServiceResponse.class,
                            subTypes = {DashboardStatsRow.class}
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid parameters provided",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = ServiceResponse.class,
                            example = "{\"success\":false,\"message\":\"Invalid threshold for category POINTS\",\"data\":null}"
                    )
            )
    )
    @GetMapping("/stats")
    public ResponseEntity<ServiceResponse<List<DashboardStatsRow>>> getDashboardStats(
            @Parameter(
                    description = "Time period for analysis (L5, L10, L15, L20, SEASON)",
                    example = "L10",
                    schema = @Schema(implementation = TimePeriod.class)
            )
            @RequestParam(defaultValue = "L10") TimePeriod timePeriod,

            @Parameter(
                    description = "Statistical category to analyze. Use 'ALL' to see all categories.",
                    example = "POINTS",
                    schema = @Schema(implementation = StatCategory.class)
            )
            @RequestParam(required = false, defaultValue = "ALL") StatCategory category,

            @Parameter(
                    description = "Minimum value threshold (e.g., 20 for 20+ points). " +
                            "Required when category is specific, ignored for 'ALL'.",
                    example = "20"
            )
            @RequestParam(required = false) Integer threshold,

            @Parameter(
                    description = "Field to sort results by (hitRate, average)",
                    example = "hitRate",
                    schema = @Schema(allowableValues = {"hitRate", "average"})
            )
            @RequestParam(defaultValue = "hitRate") String sortBy,

            @Parameter(
                    description = "Sort direction (asc, desc)",
                    example = "desc",
                    schema = @Schema(allowableValues = {"asc", "desc"})
            )
            @RequestParam(defaultValue = "desc") String sortDirection) {

        logger.info("Fetching dashboard stats - period: {}, category: {}, threshold: {}, sort: {} {}",
                timePeriod, category, threshold, sortBy, sortDirection);

        // If specific category selected but no threshold provided, use default
        if (category != StatCategory.ALL && threshold == null) {
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

        return ResponseEntity.ok(ServiceResponse.success(stats));
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