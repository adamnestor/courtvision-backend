package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.service.DataRefreshService;
import com.adamnestor.courtvision.service.util.DateUtils;
import com.adamnestor.courtvision.domain.Games;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import com.adamnestor.courtvision.repository.GamesRepository;

@RestController
@RequestMapping("/api/admin/data")
public class DataManagementController {

    private final DataRefreshService dataRefreshService;
    private final GamesRepository gamesRepository;
    private final DateUtils dateUtils;
    private static final Logger logger = LoggerFactory.getLogger(DataManagementController.class);

    @Autowired
    public DataManagementController(
            DataRefreshService dataRefreshService,
            GamesRepository gamesRepository,
            DateUtils dateUtils) {
        this.dataRefreshService = dataRefreshService;
        this.gamesRepository = gamesRepository;
        this.dateUtils = dateUtils;
    }

    @GetMapping("/games/today")
    public ResponseEntity<List<Games>> getTodaysGames() {
        var today = dateUtils.getCurrentEasternDate();
        logger.info("Searching for games on date: {}", today);
        
        // First find all games for today regardless of status
        var allGames = gamesRepository.findByGameDate(today);
        logger.info("Found {} total games for today", allGames.size());
        
        // Then find scheduled games
        List<Games> games = gamesRepository.findByGameDateAndStatus(
            today,
            "scheduled"
        );
        logger.info("Found {} scheduled games for today", games.size());
        
        return ResponseEntity.ok(games);
    }

    @PostMapping("/import/{year}/{month}")
    public ResponseEntity<String> importHistoricalDataByYearMonth(
            @PathVariable Integer year,
            @PathVariable Integer month) {
        dataRefreshService.importHistoricalDataByYearMonth(year, month);
        return ResponseEntity.ok(
                String.format("Historical data import completed for %d/%d", year, month));
    }
} 