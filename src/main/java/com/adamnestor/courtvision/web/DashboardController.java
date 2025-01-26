package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.dto.common.ServiceResponse;
import com.adamnestor.courtvision.dto.response.DashboardResponse;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ServiceResponse<List<DashboardStatsResponse>>> getDashboardStats(
        @RequestParam(required = false) String timeFrame,
        @RequestParam(defaultValue = "POINTS") String categoryStr,
        @RequestParam(required = false) Integer threshold,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDir
    ) {
        StatCategory category = StatCategory.valueOf(categoryStr.toUpperCase());

        DashboardResponse response = dashboardService.getDashboardStats(
            timeFrame,
            category,
            threshold,
            sortBy,
            sortDir
        );

        return ResponseEntity.ok(ServiceResponse.success(response.stats(), response.metadata()));
    }
}