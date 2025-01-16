package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.config.CacheConfig;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class GameStatsCacheService {
    private static final Logger logger = LoggerFactory.getLogger(GameStatsCacheService.class);

    private final GameStatsRepository gameStatsRepository;
    private final CacheMonitoringService monitoringService;

    public GameStatsCacheService(
            GameStatsRepository gameStatsRepository,
            CacheMonitoringService monitoringService) {
        this.gameStatsRepository = gameStatsRepository;
        this.monitoringService = monitoringService;
    }

    @Cacheable(value = CacheConfig.PLAYER_STATS_CACHE, keyGenerator = "cacheKeyGenerator")
    public List<GameStats> getPlayerStats(Players player, TimePeriod period) {
        logger.debug("Retrieving stats for player={} period={}", player.getId(), period);

        try {
            List<GameStats> allStats = gameStatsRepository.findPlayerRecentGames(player);
            int gameLimit = getGameLimit(period);

            List<GameStats> limitedStats = allStats.stream()
                    .limit(gameLimit)
                    .toList();

            monitoringService.recordCacheAccess(false);
            return limitedStats;
        } catch (Exception e) {
            logger.error("Error retrieving player stats: {}", e.getMessage());
            monitoringService.recordError();
            throw e;
        }
    }

    @Cacheable(value = CacheConfig.PLAYER_STATS_CACHE, keyGenerator = "cacheKeyGenerator")
    public Map<String, Double> getPlayerAverages(Players player, TimePeriod period) {
        logger.debug("Calculating averages for player={} period={}", player.getId(), period);

        try {
            List<GameStats> recentGames = gameStatsRepository.findPlayerRecentGames(player)
                    .stream()
                    .limit(getGameLimit(period))
                    .toList();

            Map<String, Double> averages = new HashMap<>();

            // Calculate averages from the limited set of games
            averages.put("points", calculateAverage(recentGames, stat -> stat.getPoints()));
            averages.put("assists", calculateAverage(recentGames, stat -> stat.getAssists()));
            averages.put("rebounds", calculateAverage(recentGames, stat -> stat.getRebounds()));

            monitoringService.recordCacheAccess(false);
            return averages;
        } catch (Exception e) {
            logger.error("Error calculating averages: {}", e.getMessage());
            monitoringService.recordError();
            throw e;
        }
    }

    private double calculateAverage(List<GameStats> games, java.util.function.Function<GameStats, Integer> statExtractor) {
        if (games.isEmpty()) {
            return 0.0;
        }
        return games.stream()
                .mapToDouble(game -> statExtractor.apply(game))
                .average()
                .orElse(0.0);
    }

    private int getGameLimit(TimePeriod period) {
        return switch (period) {
            case L5 -> 5;
            case L10 -> 10;
            case L15 -> 15;
            case L20 -> 20;
            case SEASON -> Integer.MAX_VALUE;
        };
    }
}