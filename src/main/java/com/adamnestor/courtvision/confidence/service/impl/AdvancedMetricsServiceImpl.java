package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.confidence.service.AdvancedMetricsService;
import com.adamnestor.courtvision.confidence.model.AdvancedImpact;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.AdvancedGameStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdvancedMetricsServiceImpl implements AdvancedMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedMetricsServiceImpl.class);
    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // League average constants from BallDontLie API documentation
    private static final BigDecimal LEAGUE_AVG_PIE = new BigDecimal("0.100");
    private static final BigDecimal LEAGUE_AVG_USAGE = new BigDecimal("20.00");
    private static final BigDecimal LEAGUE_AVG_TS = new BigDecimal("55.00");
    private static final BigDecimal LEAGUE_AVG_AST = new BigDecimal("15.00");
    private static final BigDecimal LEAGUE_AVG_REB = new BigDecimal("10.00");

    private final AdvancedGameStatsRepository advancedStatsRepository;

    public AdvancedMetricsServiceImpl(AdvancedGameStatsRepository advancedStatsRepository) {
        this.advancedStatsRepository = advancedStatsRepository;
    }

    @Override
    public BigDecimal calculateAdvancedImpact(Players player, Games game, StatCategory category) {
        logger.debug("Calculating advanced impact for player {} in game {} for category {}",
                player.getId(), game.getId(), category);

        AdvancedGameStats stats = getLatestAdvancedStats(player);
        if (stats == null) {
            logger.warn("No advanced stats found for player {}", player.getId());
            return new BigDecimal("50.00");
        }

        BigDecimal pieImpact = analyzePIEImpact(player, category.getDefaultThreshold(), category);
        BigDecimal usageImpact = analyzeUsageRateImpact(player, game, category);
        BigDecimal efficiencyImpact = calculateEfficiencyImpact(stats, category);

        Map<String, BigDecimal> weights = getCategoryWeights(category);

        AdvancedImpact impact = new AdvancedImpact(
                pieImpact,
                usageImpact,
                efficiencyImpact,
                category,
                weights
        );

        return impact.getOverallScore().setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal analyzePIEImpact(Players player, Integer threshold, StatCategory category) {
        AdvancedGameStats stats = getLatestAdvancedStats(player);
        if (stats == null || stats.getPie() == null) {
            return new BigDecimal("50.00");
        }

        // Normalize PIE to 0-100 scale and compare to league average
        BigDecimal normalizedPie = stats.getPie()
                .subtract(LEAGUE_AVG_PIE)
                .multiply(HUNDRED)
                .add(new BigDecimal("50.00"));

        return normalizedPie.min(HUNDRED).max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal analyzeUsageRateImpact(Players player, Games game, StatCategory category) {
        AdvancedGameStats stats = getLatestAdvancedStats(player);
        if (stats == null || stats.getUsagePercentage() == null) {
            return new BigDecimal("50.00");
        }

        // Compare to league average usage rate
        BigDecimal usageImpact = stats.getUsagePercentage()
                .divide(LEAGUE_AVG_USAGE, SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED);

        return usageImpact.min(HUNDRED).max(BigDecimal.ZERO);
    }

    @Override
    public Map<String, BigDecimal> getCategoryWeights(StatCategory category) {
        Map<String, BigDecimal> weights = new HashMap<>();

        switch (category) {
            case POINTS -> {
                weights.put("PIE", new BigDecimal("0.20"));
                weights.put("USAGE", new BigDecimal("0.30"));
                weights.put("EFFICIENCY", new BigDecimal("0.50")); // TS%
            }
            case ASSISTS -> {
                weights.put("PIE", new BigDecimal("0.10"));
                weights.put("USAGE", new BigDecimal("0.20"));
                weights.put("EFFICIENCY", new BigDecimal("0.70")); // AST%
            }
            case REBOUNDS -> {
                weights.put("PIE", new BigDecimal("0.10"));
                weights.put("USAGE", new BigDecimal("0.20"));
                weights.put("EFFICIENCY", new BigDecimal("0.70")); // REB%
            }
            default -> throw new IllegalArgumentException("Invalid category: " + category);
        }

        return weights;
    }

    @Override
    public AdvancedGameStats getLatestAdvancedStats(Players player) {
        return advancedStatsRepository.findPlayerRecentGames(player)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private BigDecimal calculateEfficiencyImpact(AdvancedGameStats stats, StatCategory category) {
        return switch (category) {
            case POINTS -> normalizeEfficiency(
                    stats.getTrueShootingPercentage(), LEAGUE_AVG_TS);
            case ASSISTS -> normalizeEfficiency(
                    stats.getAssistPercentage(), LEAGUE_AVG_AST);
            case REBOUNDS -> normalizeEfficiency(
                    stats.getReboundPercentage(), LEAGUE_AVG_REB);
            default -> throw new IllegalArgumentException("Invalid category: " + category);
        };
    }

    private BigDecimal normalizeEfficiency(BigDecimal value, BigDecimal leagueAvg) {
        if (value == null) return new BigDecimal("50.00");

        return value.divide(leagueAvg, SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .min(HUNDRED)
                .max(BigDecimal.ZERO);
    }
}