package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.client.BallDontLieClient;
import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.service.GameService;
import com.adamnestor.courtvision.service.StatsService;
import com.adamnestor.courtvision.service.AdvancedStatsService;
import com.adamnestor.courtvision.service.PlayerService;
import com.adamnestor.courtvision.repository.TeamsRepository;
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
    private final PlayerService playerService;
    private final TeamsRepository teamsRepository;

    public DataRefreshServiceImpl(
            BallDontLieClient apiClient,
            GameService gameService,
            StatsService statsService,
            AdvancedStatsService advancedStatsService,
            PlayerService playerService,
            TeamsRepository teamsRepository) {
        this.apiClient = apiClient;
        this.gameService = gameService;
        this.statsService = statsService;
        this.advancedStatsService = advancedStatsService;
        this.playerService = playerService;
        this.teamsRepository = teamsRepository;
    }

    @Scheduled(cron = "0 28 13 * * *", zone = "America/New_York")
    public void preloadPlayers() {
        logger.info("Starting data preload sequence");
        
        try {
            // Load active players for all teams
            logger.info("Loading active players");
            List<Teams> teams = teamsRepository.findAll();
            teams.forEach(team -> {
                try {
                    List<Players> players = playerService.getAndUpdatePlayersByTeam(team);
                    logger.debug("Loaded {} players for team {}", players.size(), team.getName());
                } catch (Exception e) {
                    logger.error("Error loading players for team {}: {}", team.getName(), e.getMessage());
                }
            });

            logger.info("Completed player data preload");
        } catch (Exception e) {
            logger.error("Error during player preload: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 29 13 * * *", zone = "America/New_York")
    public void updateGameResults() {
        // Now we can safely process games since teams and players are loaded
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
            });

            games.forEach(this::processGameResults);
            logger.info("Completed daily game results update. Processed {} games", games.size());
        } catch (Exception e) {
            logger.error("Error updating game results: {}", e.getMessage(), e);
        }
    }

    private void processGameResults(ApiGame game) {
        try {
            if ("Final".equals(game.getStatus())) {
                if (game.getHomeTeam() == null || game.getVisitorTeam() == null) {
                    logger.error("Missing team data for game {}, skipping", game.getId());
                    return;
                }
                String visitorTeam = game.getVisitorTeam() != null ? game.getVisitorTeam().toString() : "Unknown";
                String homeTeam = game.getHomeTeam() != null ? game.getHomeTeam().toString() : "Unknown";
                logger.debug("Processing completed game: {} with teams: visitor={}, home={}", 
                    game.getId(), 
                    visitorTeam,
                    homeTeam);
                Games gameEntity = gameService.findByExternalId(game.getId());
                if (gameEntity == null) {
                    LocalDate gameDate = LocalDate.parse(game.getDate());
                    gameService.getAndUpdateGames(gameDate);
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