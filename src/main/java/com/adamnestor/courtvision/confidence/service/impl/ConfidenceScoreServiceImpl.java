package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.confidence.service.ConfidenceScoreService;
import com.adamnestor.courtvision.confidence.service.GameContextService;
import com.adamnestor.courtvision.confidence.service.RestImpactService;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ConfidenceScoreServiceImpl implements ConfidenceScoreService {
    private static final int SCALE = 2;
    private final GameStatsRepository gameStatsRepository;
    private final GameContextService gameContextService;
    private final RestImpactService restImpactService;

    public ConfidenceScoreServiceImpl(
            GameStatsRepository gameStatsRepository,
            GameContextService gameContextService,
            RestImpactService restImpactService) {
        this.gameStatsRepository = gameStatsRepository;
        this.gameContextService = gameContextService;
        this.restImpactService = restImpactService;
    }

    @Override
    public BigDecimal calculateConfidenceScore(
            Players player,
            Games game,
            StatCategory category,
            Integer threshold,
            BigDecimal hitRate,
            int gamesCount) {

        // 1. Base Score (55%)
        BigDecimal baseScore = calculateBaseScore(hitRate, player, category, threshold, gamesCount);

        // 2. Matchup Impact (25%)
        BigDecimal matchupScore = gameContextService
                .calculateGameContext(player, game, category)
                .getOverallScore()
                .multiply(new BigDecimal("0.25"));

        // 3. Recent Form (20%)
        BigDecimal recentFormScore = calculateRecentForm(player, category)
                .multiply(new BigDecimal("0.20"));

        // Combine weighted components
        BigDecimal initialScore = baseScore
                .add(matchupScore)
                .add(recentFormScore)
                .min(new BigDecimal("100"))
                .max(BigDecimal.ZERO);

        // Apply rest multiplier
        return initialScore
                .multiply(restImpactService.calculateRestImpact(player, game).getMultiplier())
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRecentForm(Players player, StatCategory category) {
        // Get last 5 games
        List<GameStats> last5Games = gameStatsRepository.findByPlayerOrderByGameDateDesc(player, 5);

        // Get all games this season
        List<GameStats> seasonGames = gameStatsRepository.findPlayerRecentGames(player);

        if (last5Games.isEmpty() || seasonGames.isEmpty()) {
            return new BigDecimal("50.00");
        }

        // Calculate averages based on category
        double last5Avg = last5Games.stream()
                .mapToDouble(gs -> switch(category) {
                    case POINTS -> gs.getPoints();
                    case ASSISTS -> gs.getAssists();
                    case REBOUNDS -> gs.getRebounds();
                })
                .average()
                .orElse(0.0);

        double seasonAvg = seasonGames.stream()
                .mapToDouble(gs -> switch(category) {
                    case POINTS -> gs.getPoints();
                    case ASSISTS -> gs.getAssists();
                    case REBOUNDS -> gs.getRebounds();
                })
                .average()
                .orElse(0.0);

        if (seasonAvg == 0) {
            return new BigDecimal("50.00");
        }

        double percentDiff = (last5Avg - seasonAvg) / seasonAvg * 100;

        BigDecimal adjustment;
        if (percentDiff > 15) adjustment = new BigDecimal("8");
        else if (percentDiff > 10) adjustment = new BigDecimal("5");
        else if (percentDiff > 5) adjustment = new BigDecimal("3");
        else if (percentDiff < -15) adjustment = new BigDecimal("-8");
        else if (percentDiff < -10) adjustment = new BigDecimal("-5");
        else if (percentDiff < -5) adjustment = new BigDecimal("-3");
        else adjustment = BigDecimal.ZERO;

        return new BigDecimal("50").add(adjustment);
    }

    private BigDecimal calculateBaseScore(BigDecimal hitRate, Players player, StatCategory category, Integer threshold, int gamesCount) {
        // Get last 10 games
        List<GameStats> periodGames = gameStatsRepository
                .findByPlayerOrderByGameDateDesc(player, gamesCount);

        // Calculate average and how consistently they clear the threshold
        double average = periodGames.stream()
                .mapToDouble(gs -> switch(category) {
                    case POINTS -> gs.getPoints();
                    case ASSISTS -> gs.getAssists();
                    case REBOUNDS -> gs.getRebounds();
                })
                .average()
                .orElse(0.0);

        // Calculate average margin they clear threshold by when they hit
        double averageMarginWhenHit = periodGames.stream()
                .mapToDouble(gs -> {
                    int value = switch(category) {
                        case POINTS -> gs.getPoints();
                        case ASSISTS -> gs.getAssists();
                        case REBOUNDS -> gs.getRebounds();
                    };
                    return value >= threshold ? value - threshold : 0;
                })
                .filter(margin -> margin > 0)
                .average()
                .orElse(0.0);

        // Base score starts with hit rate
        BigDecimal baseScore = hitRate.multiply(new BigDecimal("0.55"));

        // Calculate margin multiplier based on how comfortably they clear it
        double marginMultiplier = Math.min(1.5, 1.0 + (averageMarginWhenHit / threshold) * 0.5);

        // Apply the multiplier to base score
        baseScore = baseScore.multiply(BigDecimal.valueOf(marginMultiplier));

        // Scale back based on threshold vs average
        double thresholdRatio = threshold / average;
        if (thresholdRatio > 1.2) { // Threshold is significantly above average
            baseScore = baseScore.multiply(BigDecimal.valueOf(0.9));  // 10% reduction
        }

        return baseScore.min(new BigDecimal("100")).max(BigDecimal.ZERO);
    }
}
