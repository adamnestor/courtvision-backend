package com.adamnestor.courtvision.confidence.util;

import com.adamnestor.courtvision.confidence.model.RestImpact;
import com.adamnestor.courtvision.domain.Games;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Utility class for calculating rest-related impacts on player performance.
 */
public class RestCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RestCalculator.class);

    private static final BigDecimal BACK_TO_BACK_MULTIPLIER = new BigDecimal("0.90");  // -10%
    private static final BigDecimal ONE_DAY_REST_MULTIPLIER = BigDecimal.ONE;          // baseline
    private static final BigDecimal TWO_DAYS_REST_MULTIPLIER = new BigDecimal("1.02"); // +2%
    private static final BigDecimal THREE_PLUS_DAYS_MULTIPLIER = new BigDecimal("1.05"); // +5%

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private RestCalculator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Calculates days of rest between games.
     */
    public static int calculateDaysOfRest(LocalDate previousGame, LocalDate currentGame) {
        if (previousGame == null || currentGame == null) {
            logger.warn("Missing date for rest calculation");
            return -1;
        }

        if (currentGame.isBefore(previousGame)) {
            throw new IllegalArgumentException("Current game date cannot be before previous game date");
        }

        return (int) ChronoUnit.DAYS.between(previousGame, currentGame);
    }

    /**
     * Gets the rest multiplier based on days of rest.
     */
    public static BigDecimal getRestMultiplier(int daysOfRest) {
        return switch (daysOfRest) {
            case 0 -> BACK_TO_BACK_MULTIPLIER;
            case 1 -> ONE_DAY_REST_MULTIPLIER;
            case 2 -> TWO_DAYS_REST_MULTIPLIER;
            default -> {
                if (daysOfRest < 0) {
                    logger.warn("Invalid days of rest: {}", daysOfRest);
                    yield ONE_DAY_REST_MULTIPLIER;
                }
                yield THREE_PLUS_DAYS_MULTIPLIER;
            }
        };
    }

    /**
     * Calculates rest impact score based on historical performance.
     */
    public static BigDecimal calculateRestImpactScore(List<Games> recentGames, int daysOfRest) {
        if (recentGames == null || recentGames.isEmpty()) {
            logger.warn("No games provided for rest impact calculation");
            return BigDecimal.ONE;
        }

        BigDecimal baseImpact = getRestMultiplier(daysOfRest);

        // Count games with similar rest patterns
        long similarRestGames = recentGames.stream()
                .filter(game -> calculateDaysOfRest(
                        game.getGameDate(),
                        game.getGameDate().plusDays(daysOfRest)
                ) == daysOfRest)
                .count();

        // Adjust impact based on historical pattern
        if (similarRestGames > 0) {
            BigDecimal historyMultiplier = BigDecimal.valueOf(similarRestGames)
                    .divide(BigDecimal.valueOf(recentGames.size()), SCALE, ROUNDING_MODE)
                    .add(BigDecimal.ONE)
                    .divide(new BigDecimal("2"), SCALE, ROUNDING_MODE);

            return baseImpact.multiply(historyMultiplier).setScale(SCALE, ROUNDING_MODE);
        }

        return baseImpact;
    }

    /**
     * Creates a RestImpact object for a game with calculated metrics.
     */
    public static RestImpact createRestImpact(LocalDate previousGame, LocalDate currentGame, List<Games> recentGames) {
        int daysOfRest = calculateDaysOfRest(previousGame, currentGame);
        BigDecimal multiplier = getRestMultiplier(daysOfRest);
        BigDecimal impactScore = calculateRestImpactScore(recentGames, daysOfRest);

        return new RestImpact(daysOfRest, multiplier, impactScore, currentGame);
    }
}