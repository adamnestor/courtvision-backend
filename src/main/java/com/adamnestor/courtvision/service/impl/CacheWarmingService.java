package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.client.BallDontLieClient;
import com.adamnestor.courtvision.service.GameService;
import com.adamnestor.courtvision.service.StatsService;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.repository.PlayersRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CacheWarmingService {
    private static final Logger logger = LoggerFactory.getLogger(CacheWarmingService.class);
    
    private final BallDontLieClient apiClient;
    private final GameService gameService;
    private final StatsService statsService;
    private final HitRateCalculationServiceImpl hitRateService;
    private final PlayersRepository playersRepository;
    
    public CacheWarmingService(
            BallDontLieClient apiClient,
            GameService gameService,
            StatsService statsService,
            HitRateCalculationServiceImpl hitRateService,
            PlayersRepository playersRepository) {
        this.apiClient = apiClient;
        this.gameService = gameService;
        this.statsService = statsService;
        this.hitRateService = hitRateService;
        this.playersRepository = playersRepository;
    }

    // Pre-warm the cache at 5 AM ET every day
    @Scheduled(cron = "0 0 5 * * *", zone = "America/New_York")
    public void warmCache() {
        logger.info("Starting cache warming process");
        try {
            warmGames();
            warmPlayerData();
            logger.info("Cache warming completed successfully");
        } catch (Exception e) {
            logger.error("Error during cache warming: {}", e.getMessage(), e);
        }
    }
    
    private void warmGames() {
        logger.debug("Warming game data");
        LocalDate today = LocalDate.now();
        
        // Get and warm today's games
        List<Games> todaysGames = gameService.getTodaysGames();
        todaysGames.forEach(game -> {
            try {
                // Warm stats for each game
                statsService.getAndUpdateGameStats(game);
                
                // Also warm tomorrow's games from API
                apiClient.getGames(today.plusDays(1));
            } catch (Exception e) {
                logger.error("Error warming data for game {}: {}", game.getId(), e.getMessage());
            }
        });
    }
    
    private void warmPlayerData() {
        logger.debug("Warming player data");
        
        gameService.getTodaysGames().stream()
            .flatMap(game -> {
                List<Players> homePlayers = playersRepository.findByTeamId(game.getHomeTeam().getId());
                List<Players> awayPlayers = playersRepository.findByTeamId(game.getAwayTeam().getId());
                return Stream.concat(homePlayers.stream(), awayPlayers.stream());
            })
            .distinct()
            .forEach(player -> {
                try {
                    hitRateService.calculateHitRate(player, StatCategory.POINTS, 20, TimePeriod.L5);
                    hitRateService.calculateHitRate(player, StatCategory.ASSISTS, 6, TimePeriod.L5);
                    hitRateService.calculateHitRate(player, StatCategory.REBOUNDS, 8, TimePeriod.L5);
                } catch (Exception e) {
                    logger.error("Error warming data for player {}: {}", player.getId(), e.getMessage());
                }
            });
    }
} 