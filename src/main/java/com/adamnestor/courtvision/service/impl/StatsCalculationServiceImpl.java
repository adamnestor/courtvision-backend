package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.service.StatsCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(timePeriod);

        List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player, startDate, endDate);
        logger.debug("Found {} games in date range", games.size());

        games = limitGamesByPeriod(games, timePeriod);
        logger.debug("After period limit: {} games", games.size());

        Map<StatCategory, BigDecimal> averages = new HashMap<>();

        if (!games.isEmpty()) {
            averages.put(StatCategory.POINTS, calculateAverage(games, StatCategory.POINTS));
            averages.put(StatCategory.ASSISTS, calculateAverage(games, StatCategory.ASSISTS));
            averages.put(StatCategory.REBOUNDS, calculateAverage(games, StatCategory.REBOUNDS));
            logger.info("Calculated averages for player: {} - Points: {}, Assists: {}, Rebounds: {}",
                    player.getId(), averages.get(StatCategory.POINTS),
                    averages.get(StatCategory.ASSISTS), averages.get(StatCategory.REBOUNDS));
        } else {
            logger.warn("No games found for player: {}", player.getId());
        }

        return averages;
    }

    @Override
    public BigDecimal getThresholdPercentage(Players player, StatCategory category,
                                             Integer threshold, TimePeriod timePeriod) {
        logger.debug("Getting threshold percentage - Player: {}, Category: {}, Threshold: {}, Period: {}",
                player.getId(), category, threshold, timePeriod);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(timePeriod);

        List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player, startDate, endDate);
        logger.debug("Found {} games in date range", games.size());

        games = limitGamesByPeriod(games, timePeriod);
        logger.debug("After period limit: {} games", games.size());

        if (games.isEmpty()) {
            logger.warn("No games found for player: {}", player.getId());
            return BigDecimal.ZERO;
        }

        long gamesMetThreshold = games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count();

        BigDecimal percentage = BigDecimal.valueOf(gamesMetThreshold)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(games.size()), 2, BigDecimal.ROUND_HALF_UP);

        logger.info("Player {} met {} {} threshold in {}% of games",
                player.getId(), threshold, category, percentage);

        return percentage;
    }

    @Override
    public boolean hasSufficientData(Players player, TimePeriod timePeriod) {
        logger.debug("Checking data sufficiency - Player: {}, Period: {}", player.getId(), timePeriod);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(timePeriod);

        List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player, startDate, endDate);
        int requiredGames = getRequiredGamesForPeriod(timePeriod);

        boolean isSufficient = games.size() >= requiredGames;
        logger.info("Data sufficiency check - Player: {}, Has {} games, Needs {}, Sufficient: {}",
                player.getId(), games.size(), requiredGames, isSufficient);

        return isSufficient;
    }

    private LocalDate calculateStartDate(TimePeriod period) {
        logger.debug("Calculating start date for period: {}", period);
        LocalDate now = LocalDate.now();
        LocalDate startDate = switch (period) {
            case L5 -> now.minusDays(30);
            case L10 -> now.minusDays(45);
            case L15 -> now.minusDays(60);
            case L20 -> now.minusDays(75);
            case SEASON -> LocalDate.of(now.getYear(), 10, 1);
        };
        logger.debug("Calculated start date: {} for period: {}", startDate, period);
        return startDate;
    }

    private List<GameStats> limitGamesByPeriod(List<GameStats> games, TimePeriod period) {
        logger.debug("Limiting {} games by period: {}", games.size(), period);
        List<GameStats> limitedGames = switch (period) {
            case L5 -> games.stream().limit(5).toList();
            case L10 -> games.stream().limit(10).toList();
            case L15 -> games.stream().limit(15).toList();
            case L20 -> games.stream().limit(20).toList();
            case SEASON -> games;
        };
        logger.debug("Limited to {} games", limitedGames.size());
        return limitedGames;
    }

    private BigDecimal calculateAverage(List<GameStats> games, StatCategory category) {
        logger.debug("Calculating average for category: {} with {} games", category, games.size());

        if (games.isEmpty()) {
            logger.debug("No games provided for average calculation");
            return BigDecimal.ZERO;
        }

        double sum = games.stream()
                .mapToInt(game -> getStatValue(game, category))
                .sum();

        BigDecimal average = BigDecimal.valueOf(sum / games.size())
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        logger.debug("Calculated average: {} for category: {}", average, category);
        return average;
    }

    private int getStatValue(GameStats game, StatCategory category) {
        int value = switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
        };
        logger.trace("Retrieved {} value: {} for game ID: {}", category, value, game.getId());
        return value;
    }

    private int getRequiredGamesForPeriod(TimePeriod period) {
        int required = switch (period) {
            case L5 -> 3;
            case L10 -> 5;
            case L15 -> 8;
            case L20 -> 10;
            case SEASON -> 15;
        };
        logger.debug("Required games for period {}: {}", period, required);
        return required;
    }
}