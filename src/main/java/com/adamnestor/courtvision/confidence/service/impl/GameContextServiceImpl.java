package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.confidence.model.GameContext;
import com.adamnestor.courtvision.confidence.service.GameContextService;
import com.adamnestor.courtvision.confidence.util.ContextCalculator;
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
import java.util.Optional;

@Service
public class GameContextServiceImpl implements GameContextService {
    private static final Logger logger = LoggerFactory.getLogger(GameContextServiceImpl.class);
    private static final int SCALE = 2;
    private static final int MIN_GAMES_FOR_MATCHUP = 3;
    private static final BigDecimal FAVORABLE_THRESHOLD = new BigDecimal("60.00");
    private static final int RECENT_GAMES_WINDOW = 30; // Days to look back for recent performance

    private final GameStatsRepository gameStatsRepository;
    private final AdvancedGameStatsRepository advancedStatsRepository;

    public GameContextServiceImpl(
            GameStatsRepository gameStatsRepository,
            AdvancedGameStatsRepository advancedStatsRepository) {
        this.gameStatsRepository = gameStatsRepository;
        this.advancedStatsRepository = advancedStatsRepository;
    }

    @Override
    public GameContext calculateGameContext(Players player, Games game, StatCategory category, Integer threshold) {
        logger.debug("Calculating game context for player {} in game {} - {} {}+",
                player.getId(), game.getId(), category, threshold);

        Teams opponent = determineOpponent(game, player.getTeam());

        BigDecimal matchupImpact = calculateMatchupImpact(player, opponent, category, threshold);
        logger.debug("Matchup impact: {}", matchupImpact);

        BigDecimal defensiveImpact = calculateDefensiveImpact(opponent, category);
        logger.debug("Defensive impact: {}", defensiveImpact);

        BigDecimal paceImpact = calculatePaceImpact(game.getHomeTeam(), opponent);
        logger.debug("Pace impact: {}", paceImpact);

        BigDecimal venueImpact = calculateVenueImpact(player, game, category);
        System.out.println("venue impact in service method: " + venueImpact);
        logger.debug("Venue impact: {}", venueImpact);

        return new GameContext(matchupImpact, defensiveImpact, paceImpact, venueImpact, category);
    }

    @Override
    public boolean isFavorableContext(Players player, Games game, StatCategory category, Integer threshold) {
        GameContext context = calculateGameContext(player, game, category, threshold);
        return context.getOverallScore().compareTo(FAVORABLE_THRESHOLD) >= 0;
    }

    private BigDecimal calculateMatchupImpact(Players player, Teams opponent, StatCategory category, Integer threshold) {
        // Get historical games against this opponent from last year
        List<GameStats> matchupGames = gameStatsRepository.findGamesByDateRange(
                        player,
                        LocalDate.now().minusYears(1),
                        LocalDate.now()
                ).stream()
                .filter(gs -> isAgainstTeam(gs.getGame(), opponent))
                .toList();

        if (matchupGames.size() < MIN_GAMES_FOR_MATCHUP) {
            logger.debug("Insufficient matchup history ({} games). Using baseline.", matchupGames.size());
            return new BigDecimal("50.00");
        }

        // Calculate success rate against this opponent
        long successfulGames = matchupGames.stream()
                .filter(gs -> metThreshold(gs, category, threshold))
                .count();

        BigDecimal successRate = BigDecimal.valueOf(successfulGames)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(matchupGames.size()), SCALE, RoundingMode.HALF_UP);

        logger.debug("Historical success rate against {}: {}", opponent.getAbbreviation(), successRate);
        return successRate;
    }

    private BigDecimal calculateDefensiveImpact(Teams opponent, StatCategory category) {
        // Get opponent's average defensive rating from last 30 days
        Optional<Double> avgDefRating = advancedStatsRepository.findTeamAverageDefensiveRating(
                opponent,
                LocalDate.now().minusDays(RECENT_GAMES_WINDOW),
                LocalDate.now()
        );

        if (avgDefRating.isEmpty()) {
            logger.debug("No defensive rating data available for {}. Using baseline.", opponent.getAbbreviation());
            return new BigDecimal("50.00");
        }

        BigDecimal defRating = BigDecimal.valueOf(avgDefRating.get());
        return ContextCalculator.calculateDefensiveImpact(defRating, category);
    }

    private BigDecimal calculatePaceImpact(Teams homeTeam, Teams awayTeam) {
        // Get recent pace for both teams
        Optional<Double> homePace = advancedStatsRepository.findTeamAveragePace(
                homeTeam,
                LocalDate.now().minusDays(RECENT_GAMES_WINDOW),
                LocalDate.now()
        );

        Optional<Double> awayPace = advancedStatsRepository.findTeamAveragePace(
                awayTeam,
                LocalDate.now().minusDays(RECENT_GAMES_WINDOW),
                LocalDate.now()
        );

        if (homePace.isEmpty() || awayPace.isEmpty()) {
            logger.debug("Insufficient pace data for teams. Using baseline.");
            return new BigDecimal("50.00");
        }

        BigDecimal paceFactor = ContextCalculator.calculatePaceFactor(
                BigDecimal.valueOf(homePace.get()),
                BigDecimal.valueOf(awayPace.get())
        );

        return ContextCalculator.normalizeScore(paceFactor);
    }

    private BigDecimal calculateVenueImpact(Players player, Games game, StatCategory category) {
        BigDecimal venueFactor = ContextCalculator.calculateVenueFactor(game, player);
        System.out.println("venue factor: " + venueFactor);
        return ContextCalculator.normalizeScore(venueFactor);
    }

    private Teams determineOpponent(Games game, Teams playerTeam) {
        return game.getHomeTeam().equals(playerTeam) ? game.getAwayTeam() : game.getHomeTeam();
    }

    private boolean isAgainstTeam(Games game, Teams team) {
        return game.getHomeTeam().equals(team) || game.getAwayTeam().equals(team);
    }

    private boolean metThreshold(GameStats stats, StatCategory category, Integer threshold) {
        return switch (category) {
            case POINTS -> stats.getPoints() >= threshold;
            case ASSISTS -> stats.getAssists() >= threshold;
            case REBOUNDS -> stats.getRebounds() >= threshold;
            default -> throw new IllegalArgumentException("Invalid category: " + category);
        };
    }
}