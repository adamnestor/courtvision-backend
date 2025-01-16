package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.config.CacheConfig;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class HitRateCacheService {
    private static final Logger logger = LoggerFactory.getLogger(HitRateCacheService.class);

    private final GameStatsRepository gameStatsRepository;
    private final CacheMonitoringService monitoringService;

    public HitRateCacheService(
            GameStatsRepository gameStatsRepository,
            CacheMonitoringService monitoringService) {
        this.gameStatsRepository = gameStatsRepository;
        this.monitoringService = monitoringService;
    }

    @Cacheable(value = CacheConfig.HIT_RATES_CACHE, keyGenerator = "cacheKeyGenerator")
    public Map<String, Object> getHitRate(Players player, StatCategory category,
                                          Integer threshold, TimePeriod period) {
        logger.debug("Calculating hit rate for player={} category={} threshold={} period={}",
                player.getId(), category, threshold, period);

        try {
            List<GameStats> recentGames = gameStatsRepository.findPlayerRecentGames(player)
                    .stream()
                    .limit(getGameLimit(period))
                    .toList();

            double hitRate = calculateHitRate(recentGames, category, threshold);
            double average = calculateAverage(recentGames, category);

            Map<String, Object> result = new HashMap<>();
            result.put("hitRate", hitRate);
            result.put("average", average);
            result.put("gamesPlayed", recentGames.size());
            result.put("period", period);
            result.put("calculatedAt", java.time.LocalDateTime.now());
            result.put("threshold", threshold);
            result.put("category", category);

            monitoringService.recordCacheAccess(false);
            return result;
        } catch (Exception e) {
            logger.error("Error calculating hit rate: {}", e.getMessage());
            monitoringService.recordError();
            throw e;
        }
    }

    private double calculateHitRate(List<GameStats> games, StatCategory category, Integer threshold) {
        if (games.isEmpty()) {
            return 0.0;
        }

        long hits = games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count();

        return (double) hits / games.size() * 100;
    }

    private double calculateAverage(List<GameStats> games, StatCategory category) {
        if (games.isEmpty()) {
            return 0.0;
        }

        return games.stream()
                .mapToInt(game -> getStatValue(game, category))
                .average()
                .orElse(0.0);
    }

    private int getStatValue(GameStats game, StatCategory category) {
        return switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
            default -> throw new IllegalArgumentException("Invalid category: " + category);
        };
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