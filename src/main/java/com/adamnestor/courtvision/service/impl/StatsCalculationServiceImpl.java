package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.service.StatsCalculationService;
import com.adamnestor.courtvision.service.cache.StatsCacheService;
import com.adamnestor.courtvision.service.util.StatAnalysisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class StatsCalculationServiceImpl implements StatsCalculationService {
    private static final Logger logger = LoggerFactory.getLogger(StatsCalculationServiceImpl.class);

    private final GameStatsRepository gameStatsRepository;
    private final StatsCacheService cacheService;

    public StatsCalculationServiceImpl(GameStatsRepository gameStatsRepository,
                                       StatsCacheService cacheService) {
        this.gameStatsRepository = gameStatsRepository;
        this.cacheService = cacheService;
    }

    @Override
    public BigDecimal getThresholdPercentage(Players player, StatCategory category,
                                             Integer threshold, TimePeriod timePeriod) {
        logger.info("Getting threshold percentage for player {} - {} {} for period {}",
                player.getId(), category, threshold, timePeriod);

        // Try to get from cache first
        Map<String, Object> cachedStats = cacheService.getHitRate(player, category,
                threshold, timePeriod);
        if (cachedStats != null && cachedStats.containsKey("hitRate")) {
            logger.debug("Cache hit for threshold percentage");
            return (BigDecimal) cachedStats.get("hitRate");
        }

        // If not in cache, calculate from stats
        List<GameStats> games = getPlayerGames(player, timePeriod);
        return StatAnalysisUtils.calculateHitRate(games, category, threshold);
    }

    @Override
    public Map<String, Object> calculateHitRate(Players player, StatCategory category,
                                                Integer threshold, TimePeriod timePeriod) {
        logger.info("Calculating hit rate for player {} - {} {} for period {}",
                player.getId(), category, threshold, timePeriod);

        // Try to get from cache first
        Map<String, Object> cachedHitRate = cacheService.getHitRate(player, category,
                threshold, timePeriod);
        if (cachedHitRate != null) {
            logger.debug("Cache hit for hit rate calculation");
            return cachedHitRate;
        }

        // If not in cache, calculate from stats
        List<GameStats> games = getPlayerGames(player, timePeriod);
        return StatAnalysisUtils.analyzeThreshold(games, category, threshold);
    }

    @Override
    public Map<StatCategory, BigDecimal> getPlayerAverages(Players player, TimePeriod timePeriod) {
        logger.info("Getting averages for player {} for period {}", player.getId(), timePeriod);

        List<GameStats> games = getPlayerGames(player, timePeriod);
        Map<StatCategory, BigDecimal> averages = Map.of(
                StatCategory.POINTS, StatAnalysisUtils.calculateAverage(games, StatCategory.POINTS),
                StatCategory.ASSISTS, StatAnalysisUtils.calculateAverage(games, StatCategory.ASSISTS),
                StatCategory.REBOUNDS, StatAnalysisUtils.calculateAverage(games, StatCategory.REBOUNDS)
        );

        logger.debug("Calculated averages: {}", averages);
        return averages;
    }

    @Override
    public boolean hasSufficientData(Players player, TimePeriod timePeriod) {
        List<GameStats> games = getPlayerGames(player, timePeriod);
        int requiredGames = getRequiredGamesForPeriod(timePeriod);

        boolean sufficient = games.size() >= requiredGames;
        logger.debug("Player {} has {} games, required {}", player.getId(),
                games.size(), requiredGames);

        return sufficient;
    }

    private List<GameStats> getPlayerGames(Players player, TimePeriod timePeriod) {
        // Try to get from cache first
        List<GameStats> cachedStats = cacheService.getPlayerStats(player, timePeriod);
        if (cachedStats != null) {
            logger.debug("Cache hit for player games");
            return cachedStats;
        }

        // If not in cache, get from repository
        int gamesNeeded = getRequiredGamesForPeriod(timePeriod);
        List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player)
                .stream()
                .limit(gamesNeeded)
                .toList();

        logger.debug("Retrieved {} games for player {}", games.size(), player.getId());
        return games;
    }

    private int getRequiredGamesForPeriod(TimePeriod period) {
        return switch (period) {
            case L5 -> 5;
            case L10 -> 10;
            case L15 -> 15;
            case L20 -> 20;
            case SEASON -> Integer.MAX_VALUE;
        };
    }
}