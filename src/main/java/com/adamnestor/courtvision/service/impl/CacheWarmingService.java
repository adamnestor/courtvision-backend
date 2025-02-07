package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.client.BallDontLieClient;
import com.adamnestor.courtvision.service.GameService;
import com.adamnestor.courtvision.service.StatsService;
import com.adamnestor.courtvision.cache.CacheSynchronizationService;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.PlayersRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.concurrent.Semaphore;

@Service
public class CacheWarmingService {
    private static final Logger logger = LoggerFactory.getLogger(CacheWarmingService.class);
    private static final int BATCH_SIZE = 20;
    private static final int API_RATE_LIMIT = 100;
    private final Semaphore rateLimiter = new Semaphore(API_RATE_LIMIT);
    
    private final BallDontLieClient apiClient;
    private final GameService gameService;
    private final StatsService statsService;
    private final HitRateCalculationServiceImpl hitRateService;
    private final PlayersRepository playersRepository;
    private final CacheSynchronizationService syncService;

    public CacheWarmingService(
            BallDontLieClient apiClient,
            GameService gameService,
            StatsService statsService,
            HitRateCalculationServiceImpl hitRateService,
            PlayersRepository playersRepository,
            CacheSynchronizationService syncService) {
        this.apiClient = apiClient;
        this.gameService = gameService;
        this.statsService = statsService;
        this.hitRateService = hitRateService;
        this.playersRepository = playersRepository;
        this.syncService = syncService;
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "America/New_York")
    public void warmCache() {
        syncService.coordinateCacheRefresh("warming", () -> {
            try {
                logger.info("Starting cache warming process");
                warmGames();
                warmPlayerData();
                logger.info("Cache warming completed successfully");
            } catch (Exception e) {
                logger.error("Error during cache warming: {}", e.getMessage(), e);
                throw e;
            }
        });
    }
    
    private void warmGames() {
        logger.debug("Warming game data");
        LocalDate today = LocalDate.now();
        
        List<Games> todaysGames = gameService.getTodaysGames();
        processBatch("games", todaysGames, game -> {
            try {
                acquireRateLimit();
                statsService.getAndUpdateGameStats(game);
                apiClient.getGames(today.plusDays(1));
            } catch (Exception e) {
                logger.error("Error warming data for game {}: {}", game.getId(), e.getMessage());
            } finally {
                rateLimiter.release();
            }
        });
    }
    
    private void warmPlayerData() {
        logger.debug("Warming player data");
        
        List<Players> players = gameService.getTodaysGames().stream()
            .flatMap(game -> Stream.concat(
                playersRepository.findByTeamId(game.getHomeTeam().getId()).stream(),
                playersRepository.findByTeamId(game.getAwayTeam().getId()).stream()))
            .distinct()
            .toList();

        processBatch("players", players, player -> {
            try {
                acquireRateLimit();
                for (TimePeriod period : TimePeriod.values()) {
                    warmPlayerStats(player, period);
                }
            } catch (Exception e) {
                logger.error("Error warming data for player {}: {}", player.getId(), e.getMessage());
            } finally {
                rateLimiter.release();
            }
        });
    }

    private void warmPlayerStats(Players player, TimePeriod period) {
        try {
            if (!hitRateService.hasSufficientData(player, period)) {
                logger.debug("Insufficient data for player {} in period {}", player.getId(), period);
                return;
            }

            List<GameStats> games = hitRateService.getPlayerGames(player, period);
            warmStatCategory(player, games, period, StatCategory.POINTS, 20);
            warmStatCategory(player, games, period, StatCategory.ASSISTS, 6);
            warmStatCategory(player, games, period, StatCategory.REBOUNDS, 8);
        } catch (Exception e) {
            logger.error("Error warming stats for player {}: {}", player.getId(), e.getMessage());
        }
    }

    private void warmStatCategory(Players player, List<GameStats> games, TimePeriod period,
                                StatCategory category, int threshold) {
        try {
            hitRateService.calculateHitRate(player, category, threshold, period);
            hitRateService.calculateStats(games, category, threshold);
        } catch (Exception e) {
            logger.error("Error warming {} stats for player {}: {}", 
                category, player.getId(), e.getMessage());
        }
    }

    private <T> void processBatch(String type, List<T> items, java.util.function.Consumer<T> processor) {
        for (int i = 0; i < items.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, items.size());
            List<T> batch = items.subList(i, end);
            
            logger.debug("Processing {} batch {}/{}", type, (i/BATCH_SIZE) + 1, 
                (items.size() + BATCH_SIZE - 1)/BATCH_SIZE);
            
            batch.parallelStream().forEach(processor);
            
            if (end < items.size()) {
                try {
                    Thread.sleep(1000); // 1 second delay between batches
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Cache warming interrupted", e);
                }
            }
        }
    }

    private void acquireRateLimit() {
        try {
            if (!rateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
                logger.warn("Rate limit reached, waiting for permit");
                rateLimiter.acquire();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Rate limit acquisition interrupted", e);
        }
    }
} 