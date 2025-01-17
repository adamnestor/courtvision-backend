package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class CacheWarmingService {
    private static final Logger logger = LoggerFactory.getLogger(CacheWarmingService.class);
    
    private final GameStatsRepository gameStatsRepository;
    private final GamesRepository gamesRepository;
    private final PlayersRepository playersRepository;
    private final CacheKeyGenerator keyGenerator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheMonitoringService monitoringService;

    public CacheWarmingService(
            GameStatsRepository gameStatsRepository,
            GamesRepository gamesRepository,
            PlayersRepository playersRepository,
            CacheKeyGenerator keyGenerator,
            RedisTemplate<String, Object> redisTemplate,
            CacheMonitoringService monitoringService) {
        this.gameStatsRepository = gameStatsRepository;
        this.gamesRepository = gamesRepository;
        this.playersRepository = playersRepository;
        this.keyGenerator = keyGenerator;
        this.redisTemplate = redisTemplate;
        this.monitoringService = monitoringService;
    }

    public void warmTodaysPlayerCache() {
        logger.info("Starting player cache warming for today's games");
        try {
            // Get today's scheduled games
            List<Games> todaysGames = gamesRepository.findByGameDateAndStatus(
                LocalDate.now(), GameStatus.SCHEDULED);
            
            if (todaysGames.isEmpty()) {
                logger.info("No games scheduled for today, skipping cache warming");
                return;
            }

            // Get unique team IDs from today's games
            Set<Long> teamIds = new HashSet<>();
            todaysGames.forEach(game -> {
                teamIds.add(game.getHomeTeam().getId());
                teamIds.add(game.getAwayTeam().getId());
            });

            // Get active players from these teams
            List<Players> players = playersRepository.findByTeamIdInAndStatus(
                teamIds, PlayerStatus.ACTIVE);

            // Warm cache for each player
            for (Players player : players) {
                warmPlayerCache(player);
            }

        } catch (Exception e) {
            logger.error("Error warming player cache: {}", e.getMessage());
            monitoringService.recordError();
        }
    }

    private void warmPlayerCache(Players player) {
        try {
            // Get player's recent stats
            List<GameStats> recentStats = gameStatsRepository.findPlayerRecentGames(player);
            
            // Cache player stats for different time periods
            String statsKey = keyGenerator.playerStatsKey(player, TimePeriod.L20);
            redisTemplate.opsForValue().set(statsKey, recentStats, 6, TimeUnit.HOURS);

            // Cache hit rates for different thresholds and categories
            warmHitRates(player, recentStats);

        } catch (Exception e) {
            logger.error("Error warming cache for player {}: {}", player.getId(), e.getMessage());
            monitoringService.recordError();
        }
    }

    private void warmHitRates(Players player, List<GameStats> stats) {
        StatCategory[] categories = StatCategory.values();
        int[] thresholds = {10, 15, 20, 25, 30, 35, 40}; // Common thresholds

        for (StatCategory category : categories) {
            for (int threshold : thresholds) {
                String hitRateKey = keyGenerator.hitRatesKey(player, category, threshold, TimePeriod.L20);
                if (!redisTemplate.hasKey(hitRateKey)) {
                    Map<String, Object> hitRateData = calculateHitRate(stats, category, threshold);
                    redisTemplate.opsForValue().set(hitRateKey, hitRateData, 24, TimeUnit.HOURS);
                }
            }
        }
    }

    private Map<String, Object> calculateHitRate(List<GameStats> stats, StatCategory category, int threshold) {
        Map<String, Object> result = new HashMap<>();
        
        if (stats.isEmpty()) {
            result.put("hitRate", 0.0);
            result.put("average", 0.0);
            result.put("gamesPlayed", 0);
        } else {
            // Calculate hit rate
            long hits = stats.stream()
                    .filter(game -> getStatValue(game, category) >= threshold)
                    .count();
            double hitRate = (hits * 100.0) / stats.size();
            
            // Calculate average
            double average = stats.stream()
                    .mapToInt(game -> getStatValue(game, category))
                    .average()
                    .orElse(0.0);
            
            result.put("hitRate", hitRate);
            result.put("average", average);
            result.put("gamesPlayed", stats.size());
        }
        
        result.put("period", TimePeriod.L20);
        result.put("threshold", threshold);
        result.put("category", category);
        result.put("calculatedAt", LocalDateTime.now());
        
        return result;
    }

    private int getStatValue(GameStats game, StatCategory category) {
        return switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
            default -> throw new IllegalArgumentException("Unsupported stat category: " + category);
        };
    }

    public void scheduledCacheWarming() {
        logger.info("Starting scheduled cache warming");
        try {
            // Warm today's games cache
            List<Games> todaysGames = gamesRepository.findByGameDateAndStatus(
                LocalDate.now(), GameStatus.SCHEDULED);
            
            String todaysGamesKey = keyGenerator.todaysGamesKey();
            redisTemplate.opsForValue().set(todaysGamesKey, todaysGames, 24, TimeUnit.HOURS);

            // Warm player caches
            warmTodaysPlayerCache();

        } catch (Exception e) {
            logger.error("Error during scheduled cache warming: {}", e.getMessage());
            monitoringService.recordError();
        }
    }

    public boolean warmTodaysGames() {
        logger.info("Starting cache warming for today's games");
        try {
            warmTodaysPlayerCache();
            scheduledCacheWarming();
            return true;
        } catch (Exception e) {
            logger.error("Error warming cache for today's games", e);
            monitoringService.recordError();
            return false;
        }
    }
}