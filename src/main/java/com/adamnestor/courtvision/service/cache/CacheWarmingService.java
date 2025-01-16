package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.config.CacheConfig;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class CacheWarmingService {
    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);

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

    /**
     * Scheduled cache warming at 4am ET daily
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "America/New_York")
    public void scheduledCacheWarming() {
        log.info("Starting scheduled cache warming");
        try {
            warmTodaysPlayerCache();
            warmCommonQueries();
            log.info("Completed scheduled cache warming");
        } catch (Exception e) {
            log.error("Error during scheduled cache warming: {}", e.getMessage());
            monitoringService.recordError();
        }
    }

    /**
     * Warms cache for today's players
     */
    public void warmTodaysPlayerCache() {
        log.info("Starting cache warming for today's players");
        List<Players> todaysPlayers;
        try {
            todaysPlayers = getTodaysPlayers();
        } catch (Exception e) {
            log.error("Error getting today's players: {}", e.getMessage());
            monitoringService.recordError();
            return;
        }

        if (CollectionUtils.isEmpty(todaysPlayers)) {
            log.warn("No players found for today's games");
            return;
        }

        int batchSize = CacheConfig.WARM_BATCH_SIZE;
        int totalPlayers = todaysPlayers.size();
        int processed = 0;

        for (int i = 0; i < todaysPlayers.size(); i += batchSize) {
            int end = Math.min(i + batchSize, totalPlayers);
            List<Players> batch = todaysPlayers.subList(i, end);

            warmPlayerBatch(batch);
            processed += batch.size();
            log.info("Warmed cache for {}/{} players", processed, totalPlayers);
        }
    }

    /**
     * Warms cache for a batch of players
     */
    private void warmPlayerBatch(List<Players> players) {
        for (Players player : players) {
            try {
                warmPlayerCache(player);
            } catch (Exception e) {
                log.error("Error warming cache for player {}: {}",
                        player.getId(), e.getMessage());
                monitoringService.recordError();
            }
        }
    }

    /**
     * Warms cache for a single player
     */
    private void warmPlayerCache(Players player) {
        // Warm recent game stats
        List<GameStats> recentStats = gameStatsRepository.findPlayerRecentGames(player);
        if (!recentStats.isEmpty()) {
            String statsKey = keyGenerator.playerStatsKey(player, TimePeriod.L10);
            redisTemplate.opsForValue().set(statsKey, recentStats,
                    CacheConfig.PLAYER_STATS_TTL_HOURS, TimeUnit.HOURS);
        }

        // Warm hit rates for common thresholds
        warmPlayerHitRates(player);
    }

    /**
     * Warms hit rates for common statistical thresholds
     */
    private void warmPlayerHitRates(Players player) {
        // Warm points thresholds
        for (Integer threshold : Arrays.asList(10, 15, 20, 25)) {
            warmCategoryHitRate(player, StatCategory.POINTS, threshold);
        }

        // Warm assists thresholds
        for (Integer threshold : Arrays.asList(2, 4, 6, 8)) {
            warmCategoryHitRate(player, StatCategory.ASSISTS, threshold);
        }

        // Warm rebounds thresholds
        for (Integer threshold : Arrays.asList(4, 6, 8, 10)) {
            warmCategoryHitRate(player, StatCategory.REBOUNDS, threshold);
        }
    }

    /**
     * Warms hit rate for a specific category and threshold
     */
    private void warmCategoryHitRate(Players player, StatCategory category, Integer threshold) {
        for (TimePeriod period : Arrays.asList(TimePeriod.L10, TimePeriod.L15)) {
            String key = keyGenerator.hitRatesKey(player, category, threshold, period);
            if (!redisTemplate.hasKey(key)) {
                // Logic to calculate hit rate would go here
                // This is just a placeholder as the actual calculation would depend on your implementation
                redisTemplate.opsForValue().set(key, calculateHitRate(player, category, threshold, period),
                        CacheConfig.DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            }
        }
    }

    /**
     * Gets list of players in today's games
     */
    private List<Players> getTodaysPlayers() {
        try {
            List<Games> todaysGames = gamesRepository.findByGameDateAndStatus(
                    LocalDate.now(), GameStatus.SCHEDULED);

            Set<Long> teamIds = new HashSet<>();
            for (Games game : todaysGames) {
                teamIds.add(game.getHomeTeam().getId());
                teamIds.add(game.getAwayTeam().getId());
            }

            return playersRepository.findByTeamIdInAndStatus(teamIds, PlayerStatus.ACTIVE);
        } catch (Exception e) {
            log.error("Error retrieving players: {}", e.getMessage());
            throw e;  // Re-throw to be handled by caller
        }
    }

    /**
     * Warms commonly accessed queries
     */
    private void warmCommonQueries() {
        // Warm today's games
        String todaysGamesKey = keyGenerator.todaysGamesKey();
        List<Games> todaysGames = gamesRepository.findByGameDateAndStatus(
                LocalDate.now(), GameStatus.SCHEDULED);
        redisTemplate.opsForValue().set(todaysGamesKey, todaysGames,
                CacheConfig.DEFAULT_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * Placeholder for hit rate calculation
     * This would be replaced with your actual hit rate calculation logic
     */
    private Object calculateHitRate(Players player, StatCategory category,
                                    Integer threshold, TimePeriod period) {
        // Placeholder - implement actual calculation logic
        return null;
    }

    public void warmTodaysGames() {
        // Implementation for warming today's games cache
        log.info("Warming cache for today's games");
    }
}