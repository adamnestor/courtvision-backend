package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.service.StatsCalculationService;
import com.adamnestor.courtvision.service.util.StatAnalysisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsCalculationServiceImpl implements StatsCalculationService {
    private static final Logger logger = LoggerFactory.getLogger(StatsCalculationServiceImpl.class);
    private final GameStatsRepository gameStatsRepository;

    public StatsCalculationServiceImpl(GameStatsRepository gameStatsRepository) {
        this.gameStatsRepository = gameStatsRepository;
        logger.info("StatsCalculationService initialized");
    }

    @Override
    public Map<StatCategory, BigDecimal> getPlayerAverages(Players player, TimePeriod timePeriod) {
        logger.debug("Getting player averages - Player: {}, Period: {}", player.getId(), timePeriod);

        List<GameStats> games = getGamesForPeriod(player, timePeriod);
        Map<StatCategory, BigDecimal> averages = new HashMap<>();

        if (!games.isEmpty()) {
            averages.put(StatCategory.POINTS, StatAnalysisUtils.calculateAverage(games, StatCategory.POINTS));
            averages.put(StatCategory.ASSISTS, StatAnalysisUtils.calculateAverage(games, StatCategory.ASSISTS));
            averages.put(StatCategory.REBOUNDS, StatAnalysisUtils.calculateAverage(games, StatCategory.REBOUNDS));
            logger.info("Calculated averages for player: {} - Points: {}, Assists: {}, Rebounds: {}",
                    player.getId(), averages.get(StatCategory.POINTS),
                    averages.get(StatCategory.ASSISTS), averages.get(StatCategory.REBOUNDS));
        } else {
            logger.warn("No recent games found for player: {}", player.getId());
        }

        return averages;
    }

    @Override
    public BigDecimal getThresholdPercentage(Players player, StatCategory category,
                                             Integer threshold, TimePeriod timePeriod) {
        logger.debug("Getting threshold percentage - Player: {}, Category: {}, Threshold: {}, Period: {}",
                player.getId(), category, threshold, timePeriod);

        List<GameStats> games = getGamesForPeriod(player, timePeriod);

        if (games.isEmpty()) {
            logger.warn("No recent games found for player: {}", player.getId());
            return BigDecimal.ZERO;
        }

        BigDecimal hitRate = StatAnalysisUtils.calculateHitRate(games, category, threshold);
        logger.info("Player {} hit rate for {} threshold {}: {}%",
                player.getId(), category, threshold, hitRate);

        return hitRate;
    }

    @Override
    public boolean hasSufficientData(Players player, TimePeriod timePeriod) {
        logger.debug("Checking if player {} has played games for period {}", player.getId(), timePeriod);

        List<GameStats> games = getGamesForPeriod(player, timePeriod);
        int requestedGames = getRequestedGamesCount(timePeriod);

        boolean hasPlayed = games.size() == requestedGames;
        logger.info("Player {} has {} games, requested {}",
                player.getId(), games.size(), requestedGames);

        return hasPlayed;
    }

    private List<GameStats> getGamesForPeriod(Players player, TimePeriod timePeriod) {
        int gamesNeeded = getRequestedGamesCount(timePeriod);

        return gameStatsRepository.findPlayerRecentGames(player)
                .stream()
                .limit(gamesNeeded)
                .toList();
    }

    private int getRequestedGamesCount(TimePeriod period) {
        return switch (period) {
            case L5 -> 5;
            case L10 -> 10;
            case L15 -> 15;
            case L20 -> 20;
            case SEASON -> Integer.MAX_VALUE;
        };
    }
}