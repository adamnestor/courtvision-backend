package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.common.ServiceResponse;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.response.PlayerStatsResponse;
import com.adamnestor.courtvision.mapper.ResponseMapper;
import com.adamnestor.courtvision.service.HitRateCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
@Tag(
        name = "Player Stats",
        description = "Detailed NBA Player Statistics APIs providing comprehensive analysis " +
                "of individual player performance across multiple statistical categories and time periods."
)
public class PlayerController {
    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);
    private final HitRateCalculationService statsService;
    private final ResponseMapper responseMapper;

    public PlayerController(HitRateCalculationService statsService, ResponseMapper responseMapper) {
        this.statsService = statsService;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "Get detailed player statistics",
            description = "Retrieves comprehensive game-by-game statistics for a specific player. " +
                    "Includes individual game performances, hit rates, statistical averages, and " +
                    "performance trends. The response contains both aggregate statistics and " +
                    "detailed breakdowns for each game within the specified time period. " +
                    "\n\nKey features:" +
                    "\n- Game-by-game performance data" +
                    "\n- Hit rate calculations for specified thresholds" +
                    "\n- Statistical averages over the time period" +
                    "\n- Home/away game indicators" +
                    "\n- Opponent information" +
                    "\n- Detailed performance metrics" +
                    "\n\nUse Cases:" +
                    "\n- Analyzing player consistency" +
                    "\n- Identifying performance trends" +
                    "\n- Evaluating matchup performance" +
                    "\n- Tracking home/away splits"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved player statistics",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = ApiResponse.class,
                            subTypes = {PlayerDetailStats.class}
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid parameters provided",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = ApiResponse.class,
                            example = "{\"success\":false,\"message\":\"Invalid threshold value for category\",\"data\":null}"
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Player not found or no data available",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = ApiResponse.class,
                            example = "{\"success\":false,\"message\":\"Player not found with id: 123\",\"data\":null}"
                    )
            )
    )
    @GetMapping("/{playerId}/stats")
    public ResponseEntity<ServiceResponse<PlayerStatsResponse>> getPlayerStats(
            @Parameter(
                    description = "Unique identifier of the player",
                    required = true,
                    example = "1",
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")
            )
            @PathVariable Long playerId,

            @Parameter(
                    description = "Time period for statistical analysis. Controls how many recent games " +
                            "are included in the analysis. Each period represents a specific number of " +
                            "recent games (e.g., L10 = last 10 games).",
                    example = "L10",
                    schema = @Schema(implementation = TimePeriod.class)
            )
            @RequestParam(defaultValue = "L10") TimePeriod timePeriod,

            @Parameter(
                    description = "Statistical category to analyze. Determines which stats are highlighted " +
                            "and which thresholds are applied. Each category has its own set of valid " +
                            "thresholds and default values.",
                    example = "POINTS",
                    schema = @Schema(implementation = StatCategory.class)
            )
            @RequestParam(defaultValue = "POINTS") StatCategory category,

            @Parameter(
                    description = "Minimum value threshold for hit rate calculation. If not provided, " +
                            "uses category defaults: Points=20, Assists=4, Rebounds=8. Must be within " +
                            "the valid range for the selected category.",
                    example = "20",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "100")
            )
            @RequestParam(required = false) Integer threshold) {

        logger.info("Fetching player stats - id: {}, period: {}, category: {}, threshold: {}",
                playerId, timePeriod, category, threshold);

        if (threshold == null) {
            threshold = switch (category) {
                case POINTS -> 20;
                case ASSISTS -> 4;
                case REBOUNDS -> 8;
                case ALL -> null;
            };
        }

        try {
            PlayerDetailStats stats = statsService.getPlayerDetailStats(
                    playerId, timePeriod, category, threshold);
            PlayerStatsResponse response = responseMapper.toPlayerStatsResponse(stats);
            return ResponseEntity.ok(ServiceResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }
}