package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.common.ApiResponse;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.service.StatsCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Stats", description = "NBA Statistics APIs")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final StatsCalculationService statsService;

    public DashboardController(StatsCalculationService statsService) {
        this.statsService = statsService;
    }

    @Operation(
            summary = "Get dashboard statistics",
            description = "Retrieves hit rates and averages for players based on specified filters. " +
                    "Shows success rates for reaching statistical thresholds over selected time periods."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved stats",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DashboardStatsRow.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid parameters provided",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<List<DashboardStatsRow>>> getDashboardStats(
            @Parameter(description = "Time period for analysis (L5, L10, L15, L20, SEASON)")
            @RequestParam(defaultValue = "L10") TimePeriod timePeriod,

            @Parameter(description = "Statistical category (ALL, POINTS, ASSISTS, REBOUNDS)")
            @RequestParam(defaultValue = "ALL") StatCategory category,

            @Parameter(description = "Minimum value threshold (e.g., 20 for 20+ points)")
            @RequestParam(required = false) Integer threshold
    ) {
        logger.info("Fetching dashboard stats - period: {}, category: {}, threshold: {}",
                timePeriod, category, threshold);

        // Validate threshold logic based on category
        if (category == StatCategory.ALL && threshold != null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Threshold should not be provided when viewing all categories"));
        }

        // If specific category selected but no threshold provided, use default
        if (category != StatCategory.ALL && threshold == null) {
            threshold = getDefaultThreshold(category);
        }

        List<DashboardStatsRow> stats = statsService.getDashboardStats(
                timePeriod,
                category,
                threshold,
                "hitRate",  // Always sort by hit rate for now
                "desc"      // Always descending
        );

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    private Integer getDefaultThreshold(StatCategory category) {
        return switch (category) {
            case POINTS -> 20;
            case ASSISTS -> 6;
            case REBOUNDS -> 8;
            case ALL -> throw new IllegalArgumentException("Default threshold not applicable for ALL category");
        };
    }
}