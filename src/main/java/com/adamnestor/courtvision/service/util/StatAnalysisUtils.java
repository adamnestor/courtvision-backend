package com.adamnestor.courtvision.service.util;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.StatCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class StatAnalysisUtils {
    private static final Logger logger = LoggerFactory.getLogger(StatAnalysisUtils.class);

    // Private constructor to prevent instantiation
    private StatAnalysisUtils() {}

    public static BigDecimal calculateAverage(List<GameStats> games, StatCategory category) {
        logger.debug("Calculating average for {} with {} games", category, games.size());

        if (games == null || games.isEmpty()) {
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

    public static BigDecimal calculateHitRate(List<GameStats> games, StatCategory category, int threshold) {
        logger.debug("Calculating hit rate for {} with threshold {}", category, threshold);

        if (games == null || games.isEmpty()) {
            logger.debug("No games provided for hit rate calculation");
            return BigDecimal.ZERO;
        }

        long hitsCount = games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count();

        BigDecimal hitRate = BigDecimal.valueOf(hitsCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(games.size()), 2, BigDecimal.ROUND_HALF_UP);

        logger.debug("Calculated hit rate: {}% for {} threshold {}", hitRate, category, threshold);
        return hitRate;
    }

    private static int getStatValue(GameStats game, StatCategory category) {
        return switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
        };
    }
}