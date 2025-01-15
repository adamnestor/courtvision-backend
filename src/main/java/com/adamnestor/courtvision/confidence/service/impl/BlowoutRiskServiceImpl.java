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
import java.time.LocalDate;
import java.util.List;

@Service
public class BlowoutRiskServiceImpl implements BlowoutRiskService {
    private static final Logger logger = LoggerFactory.getLogger(BlowoutRiskServiceImpl.class);

    private static final int SCALE = 2;
    private static final BigDecimal HIGH_RISK_THRESHOLD = new BigDecimal("60.00");
    private static final BigDecimal DEFAULT_RATING = new BigDecimal("100.00");
    private static final BigDecimal DEFAULT_PACE = new BigDecimal("100.00");
    private static final int DAYS_FOR_RECENT_STATS = 30;

    private final AdvancedGameStatsRepository advancedStatsRepository;
    private final GameStatsRepository gameStatsRepository;

    public BlowoutRiskServiceImpl(
            GameStatsRepository gameStatsRepository,
            AdvancedGameStatsRepository advancedStatsRepository) {
        this.gameStatsRepository = gameStatsRepository;
        this.advancedStatsRepository = advancedStatsRepository;
    }

    @Override
    public BigDecimal calculateBlowoutRisk(Games game) {
        logger.debug("Calculating blowout risk for game {}", game.getId());

        // Get team ratings and pace
        Teams homeTeam = game.getHomeTeam();
        Teams awayTeam = game.getAwayTeam();

        // Get team metrics
        BigDecimal homeNetRating = getTeamNetRating(homeTeam);
        BigDecimal awayNetRating = getTeamNetRating(awayTeam);
        BigDecimal homePace = getTeamPace(homeTeam);
        BigDecimal awayPace = getTeamPace(awayTeam);

        // Calculate strength differential
        BigDecimal strengthDiff = BlowoutCalculator.calculateTeamStrengthDifferential(
                homeNetRating, awayNetRating, homePace, awayPace);

        // Calculate matchup factor from historical games
        BigDecimal matchupFactor = calculateMatchupFactor(homeTeam, awayTeam);

        // Calculate base probability
        BigDecimal baseRisk = BlowoutCalculator.calculateBlowoutProbability(strengthDiff);

        // Apply matchup factor and return final risk
        return baseRisk.multiply(matchupFactor)
                .min(new BigDecimal("100"))
                .max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public boolean isHighBlowoutRisk(Games game) {
        return calculateBlowoutRisk(game).compareTo(HIGH_RISK_THRESHOLD) > 0;
    }

    private BigDecimal getTeamNetRating(Teams team) {
        List<AdvancedGameStats> recentStats = advancedStatsRepository.findTeamGamesByDateRange(
                team,
                LocalDate.now().minusDays(DAYS_FOR_RECENT_STATS),
                LocalDate.now()
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
                LocalDate.now().minusDays(DAYS_FOR_RECENT_STATS),
                LocalDate.now()
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

    private BigDecimal calculateMatchupFactor(Teams homeTeam, Teams awayTeam) {
        // Get recent matchup history
        List<Games> recentGames = gameStatsRepository.findGamesByTeams(
                homeTeam,
                awayTeam,
                LocalDate.now().minusMonths(6)
        );

        if (recentGames.isEmpty()) {
            return BigDecimal.ONE;
        }

        // Count blowout games
        int blowoutCount = (int) recentGames.stream()
                .filter(g -> BlowoutCalculator.wasBlowout(
                        g.getHomeTeamScore(),
                        g.getAwayTeamScore()
                ))
                .count();

        // Use calculator to determine matchup factor
        return BlowoutCalculator.calculateHistoricalMatchupFactor(
                blowoutCount,
                recentGames.size()
        );
    }
}