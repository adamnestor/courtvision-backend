package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.confidence.service.AdvancedMetricsService;
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

    // League average constants
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

        // Calculate impacts using the same stats object
        BigDecimal pieImpact = calculatePIEImpact(stats, category.getDefaultThreshold(), category);
        BigDecimal usageImpact = calculateUsageImpact(stats, category);
        BigDecimal efficiencyImpact = calculateEfficiencyImpact(stats, category);

        Map<String, BigDecimal> weights = getCategoryWeights(category);

        return pieImpact.multiply(weights.get("PIE"))
                .add(usageImpact.multiply(weights.get("USAGE")))
                .add(efficiencyImpact.multiply(weights.get("EFFICIENCY")))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal analyzePIEImpact(Players player, Integer threshold, StatCategory category) {
        AdvancedGameStats stats = getLatestAdvancedStats(player);
        return calculatePIEImpact(stats, threshold, category);
    }

    @Override
    public BigDecimal analyzeUsageRateImpact(Players player, Games game, StatCategory category) {
        AdvancedGameStats stats = getLatestAdvancedStats(player);
        return calculateUsageImpact(stats, category);
    }

    @Override
    public Map<String, BigDecimal> getCategoryWeights(StatCategory category) {
        Map<String, BigDecimal> weights = new HashMap<>();

        switch (category) {
            case POINTS -> {
                weights.put("PIE", new BigDecimal("0.20"));
                weights.put("USAGE", new BigDecimal("0.30"));
                weights.put("EFFICIENCY", new BigDecimal("0.50"));
            }
            case ASSISTS -> {
                weights.put("PIE", new BigDecimal("0.10"));
                weights.put("USAGE", new BigDecimal("0.20"));
                weights.put("EFFICIENCY", new BigDecimal("0.70"));
            }
            case REBOUNDS -> {
                weights.put("PIE", new BigDecimal("0.10"));
                weights.put("USAGE", new BigDecimal("0.20"));
                weights.put("EFFICIENCY", new BigDecimal("0.70"));
            }
            default -> throw new IllegalArgumentException("Invalid category: " + category);
        }

        return weights;
    }

    @Override
    public AdvancedGameStats getLatestAdvancedStats(Players player) {
        return advancedStatsRepository.findPlayerRecentGames(player, 1)
                .stream()
                .findFirst()
                .orElse(null);
    }

    // Private calculation methods that work with already fetched stats
    private BigDecimal calculatePIEImpact(AdvancedGameStats stats, Integer threshold, StatCategory category) {
        if (stats == null || stats.getPie() == null) {
            return new BigDecimal("50.00");
        }

        return stats.getPie()
                .subtract(LEAGUE_AVG_PIE)
                .multiply(HUNDRED)
                .add(new BigDecimal("50.00"))
                .min(HUNDRED)
                .max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateUsageImpact(AdvancedGameStats stats, StatCategory category) {
        if (stats == null || stats.getUsagePercentage() == null) {
            return new BigDecimal("50.00");
        }

        return stats.getUsagePercentage()
                .divide(LEAGUE_AVG_USAGE, SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .min(HUNDRED)
                .max(BigDecimal.ZERO);
    }

    private BigDecimal calculateEfficiencyImpact(AdvancedGameStats stats, StatCategory category) {
        if (stats == null) {
            return new BigDecimal("50.00");
        }

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