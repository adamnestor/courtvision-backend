package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.confidence.model.RestImpact;
import com.adamnestor.courtvision.confidence.service.RestImpactService;
import com.adamnestor.courtvision.confidence.util.RestCalculator;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RestImpactServiceImpl implements RestImpactService {
    private static final Logger logger = LoggerFactory.getLogger(RestImpactServiceImpl.class);

    private static final int RECENT_GAMES_ANALYSIS = 10;
    private static final int SCALE = 4;

    private final GameStatsRepository gameStatsRepository;
    private final GamesRepository gamesRepository;

    public RestImpactServiceImpl(GameStatsRepository gameStatsRepository,
                                 GamesRepository gamesRepository) {
        this.gameStatsRepository = gameStatsRepository;
        this.gamesRepository = gamesRepository;
    }

    @Override
    public RestImpact calculateRestImpact(Players player, Games game, StatCategory category) {
        logger.debug("Calculating rest impact for player {} in game {}", player.getId(), game.getId());

        // Find previous game
        Optional<GameStats> previousGame = findPreviousGame(player, game.getGameDate());

        if (previousGame.isEmpty()) {
            logger.debug("No previous game found, using default rest impact");
            return new RestImpact(null, BigDecimal.ONE, BigDecimal.ONE, game.getGameDate());
        }

        // Get recent games for historical analysis
        List<Games> recentGames = getRecentGames(player);

        // Calculate rest impact
        return RestCalculator.createRestImpact(
                previousGame.get().getGame().getGameDate(),
                game.getGameDate(),
                recentGames
        );
    }

    @Override
    public BigDecimal getHistoricalRestPerformance(Players player, int daysOfRest, StatCategory category) {
        logger.debug("Getting historical rest performance for player {} with {} days rest",
                player.getId(), daysOfRest);

        List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player);

        if (games.isEmpty()) {
            return BigDecimal.ONE;
        }

        // Filter games by rest days and calculate average performance
        double avgPerformance = games.stream()
                .filter(gs -> calculateRestDays(gs) == daysOfRest)
                .mapToDouble(gs -> getStatValue(gs, category))
                .average()
                .orElse(0.0);

        return BigDecimal.valueOf(avgPerformance)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public List<RestImpact> analyzeRecentRestPattern(Players player, StatCategory category) {
        logger.debug("Analyzing recent rest pattern for player {}", player.getId());

        List<GameStats> recentGames = gameStatsRepository.findPlayerRecentGames(player);
        List<RestImpact> restPatterns = new ArrayList<>();

        GameStats previousGame = null;
        for (GameStats currentGame : recentGames) {
            if (previousGame != null) {
                RestImpact impact = RestCalculator.createRestImpact(
                        previousGame.getGame().getGameDate(),
                        currentGame.getGame().getGameDate(),
                        List.of(currentGame.getGame())
                );
                restPatterns.add(impact);
            }
            previousGame = currentGame;
        }

        return restPatterns;
    }

    @Override
    public boolean isBackToBack(Games game, Players player) {
        LocalDate gameDate = game.getGameDate();
        LocalDate previousDay = gameDate.minusDays(1);
        LocalDate nextDay = gameDate.plusDays(1);

        // Check for games on adjacent days
        List<Games> adjacentGames = gamesRepository.findByGameDateBetweenAndStatus(
                previousDay, nextDay, GameStatus.FINAL);

        return adjacentGames.stream()
                .filter(g -> !g.getId().equals(game.getId()))
                .anyMatch(g -> isPlayerTeamInGame(g, player.getTeam()));
    }

    // Helper methods
    private Optional<GameStats> findPreviousGame(Players player, LocalDate currentGameDate) {
        return gameStatsRepository.findPlayerRecentGames(player).stream()
                .filter(gs -> gs.getGame().getGameDate().isBefore(currentGameDate))
                .findFirst();
    }

    private List<Games> getRecentGames(Players player) {
        return gameStatsRepository.findPlayerRecentGames(player).stream()
                .limit(RECENT_GAMES_ANALYSIS)
                .map(GameStats::getGame)
                .toList();
    }

    private int calculateRestDays(GameStats currentGame) {
        Optional<GameStats> previousGame = findPreviousGame(
                currentGame.getPlayer(),
                currentGame.getGame().getGameDate()
        );

        return previousGame.map(game ->
                RestCalculator.calculateDaysOfRest(
                        game.getGame().getGameDate(),
                        currentGame.getGame().getGameDate()
                )).orElse(-1);
    }

    private double getStatValue(GameStats game, StatCategory category) {
        return switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
            case ALL -> throw new IllegalArgumentException("Cannot get stat value for category ALL");
        };
    }

    private boolean isPlayerTeamInGame(Games game, Teams team) {
        return game.getHomeTeam().equals(team) || game.getAwayTeam().equals(team);
    }
}