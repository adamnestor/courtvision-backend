package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.client.BallDontLieClient;
import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.service.GameService;
import com.adamnestor.courtvision.service.StatsService;
import com.adamnestor.courtvision.service.AdvancedStatsService;
import com.adamnestor.courtvision.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Map;

@Service
public class DataRefreshServiceImpl {
    private static final Logger logger = LoggerFactory.getLogger(DataRefreshServiceImpl.class);
    
    private final BallDontLieClient apiClient;
    private final GameService gameService;
    private final StatsService statsService;
    private final AdvancedStatsService advancedStatsService;
    private final PlayerService playerService;

    public DataRefreshServiceImpl(
            BallDontLieClient apiClient,
            GameService gameService,
            StatsService statsService,
            AdvancedStatsService advancedStatsService,
            PlayerService playerService) {
        this.apiClient = apiClient;
        this.gameService = gameService;
        this.statsService = statsService;
        this.advancedStatsService = advancedStatsService;
        this.playerService = playerService;
    }

    @Scheduled(cron = "0 23 16 * * *", zone = "America/New_York")
    public void preloadPlayers() {
        logger.info("Starting data preload sequence");
        
        try {
            logger.info("Loading active roster players from /players/active endpoint");
            List<Players> updatedPlayers = playerService.getAndUpdateActivePlayers();
            
            // Log results
            Map<String, Long> playersByTeam = updatedPlayers.stream()
                .filter(p -> p.getTeam() != null)
                .collect(Collectors.groupingBy(
                    p -> p.getTeam().getAbbreviation(),
                    Collectors.counting()
                ));
            
            playersByTeam.forEach((team, count) -> 
                logger.info("Team {}: {} players", team, count));
            
            long playersWithoutTeam = updatedPlayers.stream()
                .filter(p -> p.getTeam() == null)
                .count();
            
            if (playersWithoutTeam > 0) {
                logger.warn("{} players found without team assignment", playersWithoutTeam);
            }

            logger.info("Completed player data preload. Updated {} players ({} with team assignments)", 
                updatedPlayers.size(), 
                updatedPlayers.size() - playersWithoutTeam);
        } catch (Exception e) {
            logger.error("Error during player preload: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 39 16 * * *", zone = "America/New_York")
    public void updateGameResults() {
        logger.info("Starting daily game results update");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        try {
            List<ApiGame> games = apiClient.getGames(yesterday);
            logger.debug("Received {} games from API", games.size());
            
            // Process each game
            games.forEach(game -> {
                String homeTeamName = game.getHomeTeam() != null ? game.getHomeTeam().getName() : "Unknown";
                String visitorTeamName = game.getVisitorTeam() != null ? game.getVisitorTeam().getName() : "Unknown";
                logger.debug("Processing game: {} - {} vs {}", 
                    game.getId(),
                    homeTeamName,
                    visitorTeamName);
                    
                // Update game info
                gameService.processGameResults(game);
                
                // If game is final, update stats
                if ("Final".equals(game.getStatus())) {
                    logger.info("Game {} is final - updating stats", game.getId());
                    Games gameEntity = gameService.findByExternalId(game.getId());
                    statsService.getAndUpdateGameStats(gameEntity);
                    advancedStatsService.getAndUpdateGameAdvancedStats(gameEntity);
                    logger.info("Stats updated successfully for game {}", game.getId());
                }
            });

            logger.info("Completed daily game results update. Processed {} games", games.size());
        } catch (Exception e) {
            logger.error("Error updating game results: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 43 16 * * *", zone = "America/New_York")
    public void updateTodaysGamesAndPlayers() {
        logger.info("Starting today's games and players update");
        try {
            List<Games> todaysGames = gameService.getAndUpdateGames(LocalDate.now());
            logger.info("Updated schedule for today. Found {} games", todaysGames.size());
            
            // Log any games that were rescheduled
            todaysGames.forEach(game -> {
                logger.debug("Game scheduled: {} vs {} at {}", 
                    game.getHomeTeam().getName(),
                    game.getAwayTeam().getName(),
                    game.getGameTime());
            });
            
            // Update player statuses based on today's games
            List<Players> allPlayers = playerService.getAllPlayers();
            Set<Long> teamsWithGames = todaysGames.stream()
                .flatMap(game -> Stream.of(
                    game.getHomeTeam().getId(),
                    game.getAwayTeam().getId()
                ))
                .collect(Collectors.toSet());
            
            allPlayers.forEach(player -> {
                // Skip players without a team
                if (player.getTeam() == null) {
                    logger.warn("Player {} has no team assigned, skipping status update", 
                        player.getFirstName() + " " + player.getLastName());
                    return;
                }
                
                boolean hasGameToday = teamsWithGames.contains(player.getTeam().getId());
                player.setStatus(hasGameToday ? PlayerStatus.ACTIVE : PlayerStatus.INACTIVE);
                playerService.updatePlayer(player);
            });
            
            logger.info("Updated player statuses. {} teams playing today", teamsWithGames.size());
            
        } catch (Exception e) {
            logger.error("Error checking today's games: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void importHistoricalData(Integer season) {
        logger.info("Starting historical data import for season {}", season);
        try {
            // First get all games for the season
            List<Games> games = gameService.getAndUpdateGamesBySeason(season);
            logger.info("Found {} games for season {}", games.size(), season);
            
            // Process only completed games
            games.stream()
                .filter(game -> "Final".equals(game.getStatus()))
                .forEach(game -> {
                    try {
                        // Get both basic and advanced stats
                        var basicStats = statsService.getAndUpdateGameStats(game);
                        var advancedStats = advancedStatsService.getAndUpdateGameAdvancedStats(game);
                        
                        logger.debug("Processed game {} - Basic stats: {}, Advanced stats: {}", 
                            game.getId(), 
                            basicStats.size(),
                            advancedStats != null ? advancedStats.size() : 0);
                    } catch (Exception e) {
                        logger.error("Error processing game {}: {}", game.getId(), e.getMessage());
                    }
                });
            
            logger.info("Completed historical data import for season {}", season);
        } catch (Exception e) {
            logger.error("Error during historical data import: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to import historical data", e);
        }
    }

    @Transactional
    public void importHistoricalDataByYearMonth(Integer year, Integer month) {
        logger.info("Starting historical data import for {}/{}", year, month);
        try {
            // Calculate start and end dates for the month
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(
                startDate.getMonth().length(startDate.isLeapYear())
            );
            
            List<Games> games = gameService.getGamesByDateRange(startDate, endDate);
            logger.info("Found {} games for {}/{}", games.size(), year, month);
            
            // Process only completed games
            games.stream()
                .filter(game -> "Final".equals(game.getStatus()))
                .forEach(game -> {
                    try {
                        var basicStats = statsService.getAndUpdateGameStats(game);
                        var advancedStats = advancedStatsService.getAndUpdateGameAdvancedStats(game);
                        
                        logger.debug("Processed game {} - Basic stats: {}, Advanced stats: {}", 
                            game.getId(), 
                            basicStats.size(),
                            advancedStats != null ? advancedStats.size() : 0);
                    } catch (Exception e) {
                        logger.error("Error processing game {}: {}", game.getId(), e.getMessage());
                    }
                });
            
            logger.info("Completed historical data import for {}/{}", year, month);
        } catch (Exception e) {
            logger.error("Error during historical data import: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to import historical data", e);
        }
    }

    @Transactional
    public void refreshDataByYearMonth(int year, int month) {
        logger.info("Starting data refresh for {}/{}", year, month);
        
        List<Games> games = gameService.getAndUpdateGamesByYearMonth(
            year,
            month
        );
        
        for (Games game : games) {
            try {
                statsService.getAndUpdateGameStats(game);
                logger.debug("Processed game: {}", game.getId());
            } catch (Exception e) {
                logger.error("Error processing game {}: {}", game.getId(), e.getMessage());
            }
        }
    }
} 