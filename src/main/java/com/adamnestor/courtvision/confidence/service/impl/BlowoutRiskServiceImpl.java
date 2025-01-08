package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.confidence.model.BlowoutImpact;
import com.adamnestor.courtvision.confidence.service.BlowoutRiskService;
import com.adamnestor.courtvision.confidence.util.BlowoutCalculator;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.AdvancedGameStatsRepository;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlowoutRiskServiceImpl implements BlowoutRiskService {
    private static final Logger logger = LoggerFactory.getLogger(BlowoutRiskServiceImpl.class);

    private static final int SCALE = 2;
    private static final int RECENT_GAMES_WINDOW = 10;
    private static final BigDecimal HIGH_RISK_THRESHOLD = new BigDecimal("60.00");
    private static final BigDecimal DEFAULT_RATING = new BigDecimal("100.00");
    private static final BigDecimal DEFAULT_PACE = new BigDecimal("100.00");

    private final GameStatsRepository gameStatsRepository;
    private final AdvancedGameStatsRepository advancedStatsRepository;

    public BlowoutRiskServiceImpl(
            GameStatsRepository gameStatsRepository,
            AdvancedGameStatsRepository advancedStatsRepository) {
        this.gameStatsRepository = gameStatsRepository;
        this.advancedStatsRepository = advancedStatsRepository;
    }

    @Override
    public BigDecimal calculateBlowoutRisk(Games game, Players player) {
        logger.debug("Calculating blowout risk for game {} and player {}",
                game.getId(), player.getId());

        // Get team advanced stats
        BigDecimal homeNetRating = getTeamNetRating(game.getHomeTeam());
        BigDecimal awayNetRating = getTeamNetRating(game.getAwayTeam());
        BigDecimal homePace = getTeamPace(game.getHomeTeam());
        BigDecimal awayPace = getTeamPace(game.getAwayTeam());

        // Calculate strength differential
        BigDecimal strengthDiff = BlowoutCalculator.calculateTeamStrengthDifferential(
                homeNetRating,
                awayNetRating,
                homePace,
                awayPace
        );

        // Get player's blowout impact
        BlowoutImpact impact = analyzePlayerBlowoutImpact(player);

        // Calculate base probability
        BigDecimal baseRisk = BlowoutCalculator.calculateBlowoutProbability(strengthDiff);

        // Adjust based on player's retention factors
        return baseRisk
                .multiply(BigDecimal.ONE.subtract(
                        impact.getPerformanceRetention().multiply(new BigDecimal("0.3"))
                ))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BlowoutImpact analyzePlayerBlowoutImpact(Players player) {
        logger.debug("Analyzing blowout impact for player {}", player.getId());

        // Get recent games
        List<GameStats> recentGames = gameStatsRepository.findPlayerRecentGames(player)
                .stream()
                .limit(RECENT_GAMES_WINDOW)
                .collect(Collectors.toList());

        // Split into blowout and normal games
        List<GameStats> blowoutGames = recentGames.stream()
                .filter(gs -> BlowoutCalculator.wasBlowout(
                        gs.getGame().getHomeTeamScore(),
                        gs.getGame().getAwayTeamScore()
                ))
                .collect(Collectors.toList());

        List<GameStats> normalGames = recentGames.stream()
                .filter(gs -> !BlowoutCalculator.wasBlowout(
                        gs.getGame().getHomeTeamScore(),
                        gs.getGame().getAwayTeamScore()
                ))
                .collect(Collectors.toList());

        // Calculate retention factors
        BigDecimal minutesRetention = BlowoutCalculator.calculateMinutesRetention(
                blowoutGames,
                normalGames
        );

        // Get latest advanced stats
        AdvancedGameStats advStats = advancedStatsRepository.findPlayerRecentGames(player)
                .stream()
                .findFirst()
                .orElse(null);

        BigDecimal performanceRetention = (advStats != null) ?
                BlowoutCalculator.calculatePerformanceRetention(
                        advStats.getPie(),
                        advStats.getUsagePercentage()
                ) :
                BigDecimal.ONE;

        BigDecimal baseRisk = calculateBaseBlowoutRisk(blowoutGames.size(), recentGames.size());

        return new BlowoutImpact(minutesRetention, performanceRetention, baseRisk);
    }

    @Override
    public boolean isHighBlowoutRisk(Games game) {
        return calculateBlowoutRisk(game, null).compareTo(HIGH_RISK_THRESHOLD) > 0;
    }

    private BigDecimal getTeamNetRating(Teams team) {
        List<AdvancedGameStats> recentStats = advancedStatsRepository.findTeamGamesByDateRange(
                team,
                java.time.LocalDate.now().minusDays(30),
                java.time.LocalDate.now()
        );

        if (recentStats.isEmpty()) {
            return DEFAULT_RATING;
        }

        double avgNetRating = recentStats.stream()
                .mapToDouble(stats -> stats.getNetRating().doubleValue())
                .average()
                .orElse(DEFAULT_RATING.doubleValue());

        return BigDecimal.valueOf(avgNetRating).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal getTeamPace(Teams team) {
        List<AdvancedGameStats> recentStats = advancedStatsRepository.findTeamGamesByDateRange(
                team,
                java.time.LocalDate.now().minusDays(30),
                java.time.LocalDate.now()
        );

        if (recentStats.isEmpty()) {
            return DEFAULT_PACE;
        }

        double avgPace = recentStats.stream()
                .mapToDouble(stats -> stats.getPace().doubleValue())
                .average()
                .orElse(DEFAULT_PACE.doubleValue());

        return BigDecimal.valueOf(avgPace).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBaseBlowoutRisk(int blowoutGames, int totalGames) {
        if (totalGames == 0) {
            return new BigDecimal("0.50");
        }

        return BigDecimal.valueOf(blowoutGames)
                .divide(BigDecimal.valueOf(totalGames), SCALE, RoundingMode.HALF_UP);
    }
}