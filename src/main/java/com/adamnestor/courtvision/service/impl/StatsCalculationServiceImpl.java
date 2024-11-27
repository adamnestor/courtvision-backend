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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public Map<String, Object> calculateHitRate(Players player, StatCategory category,
                                                Integer threshold, TimePeriod timePeriod) {
        logger.info("Calculating hit rate for player {} - {} {} for period {}",
                player.getId(), category, threshold, timePeriod);

        // Validate the threshold
        if (threshold == null || threshold < 0) {
            throw new IllegalArgumentException("Threshold must be a non-negative value.");
        }

        // Try to get from cache first
        Map<String, Object> cachedHitRate = cacheService.getHitRate(player, category,
                threshold, timePeriod);
        if (cachedHitRate != null) {
            logger.debug("Cache hit for hit rate calculation");
            return cachedHitRate;
        }

        // If not in cache, calculate from stats
        List<GameStats> games = getPlayerGames(player, timePeriod);
        logger.debug("Got {} games before analysis", games.size());  // Debug
        if (!games.isEmpty()) {  // Debug
            logger.debug("First game points: {}", games.get(0).getPoints());
        }
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

    /**
     * Helper method to get player games either from cache or repository
     */
    private List<GameStats> getPlayerGames(Players player, TimePeriod timePeriod) {
        logger.debug("\n=== Getting Player Games ===");
        logger.debug("Player ID: {}", player.getId());
        logger.debug("Time Period: {}", timePeriod);

        // Try to get from cache first
        List<GameStats> cachedStats = cacheService.getPlayerStats(player, timePeriod);
        if (cachedStats != null) {
            logger.debug("Cache hit - Returning {} games from cache", cachedStats.size());
            return cachedStats;
        }
        logger.debug("Cache miss - Proceeding to repository");

        // If not in cache, get from repository
        int gamesNeeded = getRequiredGamesForPeriod(timePeriod);
        logger.debug("Games needed for period {}: {}", timePeriod, gamesNeeded);

        List<GameStats> repoGames = gameStatsRepository.findPlayerRecentGames(player);
        if (repoGames == null) {
            logger.warn("Repository returned null");
            return Collections.emptyList();
        }

        logger.debug("Repository returned {} games", repoGames.size());
        if (!repoGames.isEmpty()) {
            logger.debug("First game points from repo: {}", repoGames.get(0).getPoints());
        }

        // Apply limit
        List<GameStats> games = repoGames.stream()
                .limit(gamesNeeded)
                .collect(Collectors.toList());

        logger.debug("After applying limit: {} games", games.size());
        if (!games.isEmpty()) {
            logger.debug("First game points after limit: {}", games.get(0).getPoints());
        }

        return games;
    }

    /**
     * Helper method to determine required games for time period
     */
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