package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class HitRateCacheService {
    private final GameStatsRepository gameStatsRepository;
    private final CacheMonitoringService monitoringService;

    public HitRateCacheService(GameStatsRepository gameStatsRepository, CacheMonitoringService monitoringService) {
        this.gameStatsRepository = gameStatsRepository;
        this.monitoringService = monitoringService;
    }

    public Map<String, Object> getHitRate(Players player, StatCategory category, int threshold, TimePeriod period) {
        if (category == null) {
            monitoringService.recordError();
            throw new IllegalArgumentException("Category cannot be null");
        }

        try {
            List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player)
                    .stream()
                    .limit(getGameLimit(period))
                    .toList();

            Map<String, Object> result = new HashMap<>();
            result.put("hitRate", calculateHitRate(games, category, threshold));
            result.put("average", calculateAverage(games, category));
            result.put("gamesPlayed", games.size());
            result.put("period", period);
            result.put("threshold", threshold);
            result.put("category", category);
            result.put("calculatedAt", LocalDateTime.now());

            monitoringService.recordCacheAccess(false);
            return result;
        } catch (Exception e) {
            monitoringService.recordError();
            throw e;
        }
    }

    private double calculateHitRate(List<GameStats> games, StatCategory category, int threshold) {
        if (games.isEmpty()) return 0.0;
        long hits = games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count();
        return (hits * 100.0) / games.size();
    }

    private double calculateAverage(List<GameStats> games, StatCategory category) {
        if (games.isEmpty()) return 0.0;
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
            default -> throw new IllegalArgumentException("Unsupported stat category: " + category);
        };
    }

    private int getGameLimit(TimePeriod period) {
        return switch (period) {
            case L5 -> 5;
            case L10 -> 10;
            case L15 -> 15;
            case L20 -> 20;
            case SEASON -> Integer.MAX_VALUE;
            default -> throw new IllegalArgumentException("Unsupported time period: " + period);
        };
    }
}