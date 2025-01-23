package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.common.ServiceResponse;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import com.adamnestor.courtvision.service.HitRateCalculationService;
import com.adamnestor.courtvision.mapper.ResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "NBA Statistics Dashboard APIs")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final HitRateCalculationService statsService;
    private final ResponseMapper responseMapper;

    public DashboardController(HitRateCalculationService statsService, ResponseMapper responseMapper) {
        this.statsService = statsService;
        this.responseMapper = responseMapper;
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
                            subTypes = {DashboardStatsResponse.class}
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
    public ResponseEntity<ServiceResponse<List<DashboardStatsResponse>>> getDashboardStats(
            @RequestParam(defaultValue = "L10") TimePeriod timePeriod,
            @RequestParam(defaultValue = "ALL") StatCategory category,
            @RequestParam(required = false) Integer threshold,
            @RequestParam(defaultValue = "hitRate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        try {
            logger.info("Getting dashboard stats for period: {}, category: {}", timePeriod, category);
            
            List<DashboardStatsRow> stats = statsService.getDashboardStats(
                    timePeriod, category, threshold, sortBy, sortDirection);
            
            logger.info("Found {} stats rows", stats.size());
            
            List<DashboardStatsResponse> response = stats.stream()
                    .map(responseMapper::toDashboardResponse)
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(ServiceResponse.success(response));
        } catch (Exception e) {
            logger.error("Error getting dashboard stats", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }
}