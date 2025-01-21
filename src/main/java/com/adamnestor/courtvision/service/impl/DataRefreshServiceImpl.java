package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.client.BallDontLieClient;
import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.service.GameService;
import com.adamnestor.courtvision.service.StatsService;
import com.adamnestor.courtvision.service.AdvancedStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DataRefreshServiceImpl {
    private static final Logger logger = LoggerFactory.getLogger(DataRefreshServiceImpl.class);
    
    private final BallDontLieClient apiClient;
    private final GameService gameService;
    private final StatsService statsService;
    private final AdvancedStatsService advancedStatsService;

    public DataRefreshServiceImpl(
            BallDontLieClient apiClient,
            GameService gameService,
            StatsService statsService,
            AdvancedStatsService advancedStatsService) {
        this.apiClient = apiClient;
        this.gameService = gameService;
        this.statsService = statsService;
        this.advancedStatsService = advancedStatsService;
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "America/New_York")
    public void updateGameResults() {
        logger.info("Starting daily game results update");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        try {
            List<ApiGame> games = apiClient.getGames(yesterday);
            games.forEach(this::processGameResults);
            logger.info("Completed daily game results update. Processed {} games", games.size());
        } catch (Exception e) {
            logger.error("Error updating game results: {}", e.getMessage(), e);
        }
    }

    private void processGameResults(ApiGame game) {
        try {
            if ("Final".equals(game.getStatus())) {
                logger.debug("Processing completed game: {}", game.getId());
                Games gameEntity = gameService.findByExternalId(game.getId());
                if (gameEntity == null) {
                    gameService.getAndUpdateGames(game.getDate());
                    gameEntity = gameService.findByExternalId(game.getId());
                }
                
                var basicStats = statsService.getAndUpdateGameStats(gameEntity);
                var advancedStats = advancedStatsService.getAndUpdateGameAdvancedStats(gameEntity);
                
                verifyPlayerMappings(basicStats);
                if (advancedStats == null || advancedStats.isEmpty()) {
                    logger.error("Missing advanced stats for game {}", game.getId());
                    resyncGameData(gameEntity);
                }
            } else {
                logger.debug("Skipping non-final game: {}", game.getId());
            }
        } catch (Exception e) {
            logger.error("Error processing game {}: {}", game.getId(), e.getMessage(), e);
        }
    }

    @Transactional
    private void resyncGameData(Games game) {
        logger.info("Resyncing data for game {}", game.getId());
        try {
            statsService.getAndUpdateGameStats(game);
            advancedStatsService.getAndUpdateGameAdvancedStats(game);
            logger.info("Successfully resynced data for game {}", game.getId());
        } catch (Exception e) {
            logger.error("Error resyncing data for game {}: {}", game.getId(), e.getMessage(), e);
        }
    }

    private void verifyPlayerMappings(List<GameStats> stats) {
        stats.forEach(stat -> {
            if (stat.getPlayer() == null || stat.getPlayer().getExternalId() == null) {
                logger.error("Invalid player mapping in stats for game {}", stat.getGame().getId());
                resyncGameData(stat.getGame());
            }
        });
    }
} 