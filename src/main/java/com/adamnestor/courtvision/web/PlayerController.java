package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.common.ApiResponse;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.service.StatsCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Stats", description = "NBA Statistics APIs")
public class PlayerController {
    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);
    private final StatsCalculationService statsService;

    public PlayerController(StatsCalculationService statsService) {
        this.statsService = statsService;
    }

    @Operation(
            summary = "Get player statistics",
            description = "Retrieves detailed game-by-game statistics for a specific player. " +
                    "Includes individual game performances, hit rates, and statistical averages. " +
                    "Supports filtering by time period and statistical category."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved player stats",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PlayerDetailStats.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid parameters provided",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Player not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/{playerId}/stats")
    public ResponseEntity<ApiResponse<PlayerDetailStats>> getPlayerStats(
            @Parameter(
                    description = "ID of the player",
                    required = true,
                    example = "1"
            )
            @PathVariable Long playerId,

            @Parameter(
                    description = "Time period for analysis (L5, L10, L15, L20, SEASON)",
                    example = "L10"
            )
            @RequestParam(defaultValue = "L10") TimePeriod timePeriod,

            @Parameter(
                    description = "Statistical category (POINTS, ASSISTS, REBOUNDS)",
                    example = "POINTS"
            )
            @RequestParam(defaultValue = "POINTS") StatCategory category,

            @Parameter(
                    description = "Minimum value threshold (e.g., 20 for 20+ points)",
                    example = "20"
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
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}