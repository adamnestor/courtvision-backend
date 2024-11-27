package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.service.StatsCalculationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsCalculationServiceImpl implements StatsCalculationService {
    private final GameStatsRepository gameStatsRepository;

    public StatsCalculationServiceImpl(GameStatsRepository gameStatsRepository) {
        this.gameStatsRepository = gameStatsRepository;
    }

    @Override
    public Map<StatCategory, BigDecimal> getPlayerAverages(Players player, TimePeriod timePeriod) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(timePeriod);

        List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player, startDate, endDate);
        games = limitGamesByPeriod(games, timePeriod);

        Map<StatCategory, BigDecimal> averages = new HashMap<>();

        if (!games.isEmpty()) {
            averages.put(StatCategory.POINTS, calculateAverage(games, StatCategory.POINTS));
            averages.put(StatCategory.ASSISTS, calculateAverage(games, StatCategory.ASSISTS));
            averages.put(StatCategory.REBOUNDS, calculateAverage(games, StatCategory.REBOUNDS));
        }

        return averages;
    }

    @Override
    public BigDecimal getThresholdPercentage(Players player, StatCategory category,
                                             Integer threshold, TimePeriod timePeriod) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(timePeriod);

        List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player, startDate, endDate);
        games = limitGamesByPeriod(games, timePeriod);

        if (games.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long gamesMetThreshold = games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count();

        return BigDecimal.valueOf(gamesMetThreshold)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(games.size()), 2, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public boolean hasSufficientData(Players player, TimePeriod timePeriod) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(timePeriod);

        List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player, startDate, endDate);
        int requiredGames = getRequiredGamesForPeriod(timePeriod);

        return games.size() >= requiredGames;
    }

    private LocalDate calculateStartDate(TimePeriod period) {
        LocalDate now = LocalDate.now();
        return switch (period) {
            case L5 -> now.minusDays(30);  // Buffer for finding last 5 games
            case L10 -> now.minusDays(45); // Buffer for finding last 10 games
            case L15 -> now.minusDays(60); // Buffer for finding last 15 games
            case L20 -> now.minusDays(75); // Buffer for finding last 20 games
            case SEASON -> LocalDate.of(now.getYear(), 10, 1); // Season start
        };
    }

    private List<GameStats> limitGamesByPeriod(List<GameStats> games, TimePeriod period) {
        return switch (period) {
            case L5 -> games.stream().limit(5).toList();
            case L10 -> games.stream().limit(10).toList();
            case L15 -> games.stream().limit(15).toList();
            case L20 -> games.stream().limit(20).toList();
            case SEASON -> games;
        };
    }

    private BigDecimal calculateAverage(List<GameStats> games, StatCategory category) {
        double sum = games.stream()
                .mapToInt(game -> getStatValue(game, category))
                .sum();

        return BigDecimal.valueOf(sum / games.size())
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private int getStatValue(GameStats game, StatCategory category) {
        return switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
        };
    }

    private int getRequiredGamesForPeriod(TimePeriod period) {
        return switch (period) {
            case L5 -> 3;
            case L10 -> 5;
            case L15 -> 8;
            case L20 -> 10;
            case SEASON -> 15;
        };
    }
}