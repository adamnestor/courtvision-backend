package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.dto.common.ServiceResponse;
import com.adamnestor.courtvision.dto.DashboardResponse;
import com.adamnestor.courtvision.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "NBA Statistics Dashboard APIs")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get dashboard statistics")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved dashboard statistics",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ServiceResponse.class)
            )
    )
    @GetMapping("/stats")
    public ResponseEntity<ServiceResponse<DashboardResponse>> getDashboardStats(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String timeFrame,     // L5, L10, L15, L20, SEASON
        @RequestParam(required = false) String category,      // POINTS, ASSISTS, REBOUNDS
        @RequestParam(required = false) String threshold,     // 10+, 15+, 20+, 25+
        @RequestParam(required = false) String sortBy,        // hitRate, confidenceScore
        @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(
            ServiceResponse.success(
                dashboardService.getDashboardStats(
                    page, size, timeFrame, category, 
                    threshold, sortBy, sortDir
                )
            )
        );
    }
}