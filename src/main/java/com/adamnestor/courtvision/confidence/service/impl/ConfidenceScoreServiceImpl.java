package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.confidence.model.GameContext;
import com.adamnestor.courtvision.confidence.service.GameContextService;
import com.adamnestor.courtvision.confidence.model.RestImpact;
import com.adamnestor.courtvision.confidence.service.RestImpactService;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.AdvancedGameStatsRepository;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.confidence.service.ConfidenceScoreService;
import com.adamnestor.courtvision.confidence.service.AdvancedMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConfidenceScoreServiceImpl implements ConfidenceScoreService {
    private static final Logger logger = LoggerFactory.getLogger(ConfidenceScoreServiceImpl.class);

    private static final int SCALE = 2;
    private static final double DECAY_FACTOR = 0.15;
    private static final int RECENT_GAMES_COUNT = 10;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final GameStatsRepository gameStatsRepository;
    private final AdvancedGameStatsRepository advancedStatsRepository;
    private final RestImpactService restImpactService;
    private final GameContextService gameContextService;
    private final AdvancedMetricsService advancedMetricsService;

    public ConfidenceScoreServiceImpl(
            GameStatsRepository gameStatsRepository,
            AdvancedGameStatsRepository advancedStatsRepository,
            RestImpactService restImpactService,
            GameContextService gameContextService,
            AdvancedMetricsService advancedMetricsService) {
        this.gameStatsRepository = gameStatsRepository;
        this.advancedStatsRepository = advancedStatsRepository;
        this.restImpactService = restImpactService;
        this.gameContextService = gameContextService;
        this.advancedMetricsService = advancedMetricsService;
    }

    @Override
    public BigDecimal calculateConfidenceScore(Players player, Games game, Integer threshold, StatCategory category) {
        logger.info("Calculating confidence score for player {} - {} {} in game {}",
                player.getId(), category, threshold, game.getId());

        // Calculate each component using appropriate service
        BigDecimal recentPerf = calculateRecentPerformance(player, threshold, category);
        BigDecimal advancedImpact = calculateAdvancedImpact(player, game, category);
        GameContext gameContext = gameContextService.calculateGameContext(player, game, category, threshold);
        RestImpact restImpact = restImpactService.calculateRestImpact(player, game, category);

        // Calculate base confidence score with weights
        BigDecimal baseConfidence = recentPerf.multiply(new BigDecimal("0.35"))
                .add(advancedImpact.multiply(new BigDecimal("0.30")))
                .add(gameContext.getOverallScore().multiply(new BigDecimal("0.35")))
                .multiply(restImpact.getMultiplier());

        // Apply blowout risk adjustment if necessary
        BigDecimal blowoutRisk = calculateBlowoutRisk(game);
        if (blowoutRisk.compareTo(new BigDecimal("60")) > 0) {
            baseConfidence = adjustForBlowoutRisk(baseConfidence, blowoutRisk, player, threshold, category);
        }

        return baseConfidence.setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateRecentPerformance(Players player, Integer threshold, StatCategory category) {
        List<GameStats> recentGames = gameStatsRepository.findPlayerRecentGames(player)
                .stream()
                .limit(RECENT_GAMES_COUNT)
                .collect(Collectors.toList());

        if (recentGames.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double weightedSum = 0;
        double totalWeight = 0;

        for (int i = 0; i < recentGames.size(); i++) {
            GameStats game = recentGames.get(i);
            double weight = Math.exp(-DECAY_FACTOR * i);

            double hitWeight = calculateHitWeight(game, threshold, category);
            weightedSum += hitWeight * weight;
            totalWeight += weight;
        }

        return BigDecimal.valueOf(weightedSum / totalWeight * 100)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateAdvancedImpact(Players player, Games game, StatCategory category) {
        logger.debug("Calculating advanced impact for player {} in game {} for category {}",
                player.getId(), game.getId(), category);

        // Use the AdvancedMetricsService for calculations
        return advancedMetricsService.calculateAdvancedImpact(player, game, category)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateBlowoutRisk(Games game) {
        // Placeholder for Day 5 implementation
        return new BigDecimal("50.00");
    }

    @Override
    public BigDecimal calculateGameContext(Players player, Games game, StatCategory category) {
        // Use the GameContextService and convert GameContext to a single score
        GameContext context = gameContextService.calculateGameContext(
                player, game, category, category.getDefaultThreshold());
        return context.getOverallScore().setScale(SCALE, RoundingMode.HALF_UP);
    }

    private double calculateHitWeight(GameStats game, Integer threshold, StatCategory category) {
        int actualValue = switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
            case ALL -> throw new IllegalArgumentException("Cannot calculate hit weight for category ALL");
        };

        if (actualValue >= threshold) {
            return 1.0;
        } else {
            // Near miss calculation
            double missMargin = (actualValue - threshold) / (double) threshold;
            return Math.max(0.0, 1.0 + missMargin);
        }
    }

    private BigDecimal adjustForBlowoutRisk(BigDecimal baseConfidence, BigDecimal blowoutRisk,
                                            Players player, Integer threshold, StatCategory category) {
        // Placeholder for Day 5 implementation
        return baseConfidence;
    }
}