package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.common.ApiResponse;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.service.StatsCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);
    private final StatsCalculationService statsService;

    public PlayerController(StatsCalculationService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/{playerId}/stats")
    public ResponseEntity<ApiResponse<PlayerDetailStats>> getPlayerStats(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "L10") TimePeriod timePeriod,
            @RequestParam(defaultValue = "POINTS") StatCategory category,
            @RequestParam(required = false) Integer threshold) {

        logger.info("Fetching player stats - id: {}, period: {}, category: {}, threshold: {}",
                playerId, timePeriod, category, threshold);

        // Set default threshold if not provided
        if (threshold == null) {
            threshold = switch (category) {
                case POINTS -> 20;
                case ASSISTS -> 4;
                case REBOUNDS -> 8;
            };
        }

        PlayerDetailStats stats = statsService.getPlayerDetailStats(
                playerId, timePeriod, category, threshold);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}