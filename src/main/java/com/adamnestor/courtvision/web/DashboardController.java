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
            @Parameter(
                    description = "Time period for analysis (L5, L10, L15, L20, SEASON)",
                    example = "L10"
            )
            @RequestParam(defaultValue = "L10") TimePeriod timePeriod,

            @Parameter(
                    description = "Statistical category (POINTS, ASSISTS, REBOUNDS)",
                    example = "POINTS"
            )
            @RequestParam(required = false) StatCategory category,

            @Parameter(
                    description = "Minimum value threshold (e.g., 20 for 20+ points)",
                    example = "20"
            )
            @RequestParam(required = false) Integer threshold,

            @Parameter(
                    description = "Sort field (hitRate, average, gamesAnalyzed)",
                    example = "hitRate"
            )
            @RequestParam(defaultValue = "hitRate") String sortBy,

            @Parameter(
                    description = "Sort direction (asc, desc)",
                    example = "desc"
            )
            @RequestParam(defaultValue = "desc") String sortDirection) {

        logger.info("Fetching dashboard stats - period: {}, category: {}, threshold: {}, sort: {} {}",
                timePeriod, category, threshold, sortBy, sortDirection);

        if (category == null) {
            category = StatCategory.POINTS;
        }

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