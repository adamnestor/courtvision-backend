package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.mapper.DashboardMapper;
import com.adamnestor.courtvision.mapper.PlayerMapper;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.HitRateCalculationService;
import com.adamnestor.courtvision.service.cache.StatsCacheService;
import com.adamnestor.courtvision.service.util.DateUtils;
import com.adamnestor.courtvision.service.util.StatAnalysisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class HitRateCalculationServiceImpl implements HitRateCalculationService {
    private static final Logger logger = LoggerFactory.getLogger(HitRateCalculationServiceImpl.class);

    private final GameStatsRepository gameStatsRepository;
    private final GamesRepository gamesRepository;
    private final StatsCacheService cacheService;
    private final PlayersRepository playersRepository;
    private final DashboardMapper dashboardMapper;
    private final PlayerMapper playerMapper;
    private final DateUtils dateUtils;

    public HitRateCalculationServiceImpl(
            GameStatsRepository gameStatsRepository,
            GamesRepository gamesRepository,
            StatsCacheService cacheService,
            PlayersRepository playersRepository,
            DashboardMapper dashboardMapper,
            PlayerMapper playerMapper,
            DateUtils dateUtils) {
        this.gameStatsRepository = gameStatsRepository;
        this.gamesRepository = gamesRepository;
        this.cacheService = cacheService;
        this.playersRepository = playersRepository;
        this.dashboardMapper = dashboardMapper;
        this.playerMapper = playerMapper;
        this.dateUtils = dateUtils;
    }

    @Override
    public Map<String, Object> calculateHitRate(Players player, StatCategory category,
                                                Integer threshold, TimePeriod timePeriod) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null.");
        }
        if (category == null) {
            throw new IllegalArgumentException("StatCategory cannot be null.");
        }
        if (timePeriod == null) {
            throw new IllegalArgumentException("Time period cannot be null.");
        }
        if (threshold == null || threshold <= 0 || threshold > 51) {
            throw new IllegalArgumentException("Threshold must be a non-negative value but less than 51.");
        }

        logger.info("Calculating hit rate for player {} - {} {} for period {}",
                player.getId(), category, threshold, timePeriod);

        // Try to get from cache first
        Map<String, Object> cachedHitRate = cacheService.getHitRate(player, category,
                threshold, timePeriod);
        if (cachedHitRate != null) {
            logger.debug("Cache hit for hit rate calculation");
            return cachedHitRate;
        }

        // If not in cache, calculate from stats
        List<GameStats> games = getPlayerGames(player, timePeriod);
        logger.debug("Got {} games before analysis", games.size());
        if (!games.isEmpty()) {
            logger.debug("First game points: {}", games.get(0).getPoints());
        }
        return StatAnalysisUtils.analyzeThreshold(games, category, threshold);
    }

    @Override
    public Map<StatCategory, BigDecimal> getPlayerAverages(Players player, TimePeriod timePeriod) {
        if (timePeriod == null) {
            throw new IllegalArgumentException("Time period cannot be null");
        }
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        logger.info("Getting averages for player {} for period {}", player.getId(), timePeriod);

        List<GameStats> games = getPlayerGames(player, timePeriod);
        Map<StatCategory, BigDecimal> averages = Map.of(
                StatCategory.POINTS, StatAnalysisUtils.calculateAverage(games, StatCategory.POINTS),
                StatCategory.ASSISTS, StatAnalysisUtils.calculateAverage(games, StatCategory.ASSISTS),
                StatCategory.REBOUNDS, StatAnalysisUtils.calculateAverage(games, StatCategory.REBOUNDS)
        );

        logger.debug("Calculated averages: {}", averages);
        return averages;
    }

    @Override
    public boolean hasSufficientData(Players player, TimePeriod timePeriod) {
        List<GameStats> games = getPlayerGames(player, timePeriod);
        int requiredGames = getRequiredGamesForPeriod(timePeriod);

        boolean sufficient = games.size() >= requiredGames;
        logger.debug("Player {} has {} games, required {}", player.getId(),
                games.size(), requiredGames);

        return sufficient;
    }

    @Override
    public List<DashboardStatsRow> getDashboardStats(
            TimePeriod timePeriod,
            StatCategory category,
            Integer threshold,
            String sortBy,
            String sortDirection) {

        // Get today's games
        List<Games> todaysGames = gamesRepository.findByGameDateAndStatus(
                dateUtils.getCurrentEasternDate(),
                GameStatus.SCHEDULED
        );

        if (todaysGames.isEmpty()) {
            return Collections.emptyList();
        }

        List<DashboardStatsRow> stats = new ArrayList<>();

        for (Players player : getTodaysPlayers(todaysGames)) {
            // Find this player's game for today
            Games playerGame = todaysGames.stream()
                    .filter(game -> game.getHomeTeam().equals(player.getTeam())
                            || game.getAwayTeam().equals(player.getTeam()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Player game not found"));

            // Calculate opponent string (vs LAL or @ LAL)
            String opponent = playerGame.getHomeTeam().equals(player.getTeam())
                    ? "vs " + playerGame.getAwayTeam().getAbbreviation()
                    : "@ " + playerGame.getHomeTeam().getAbbreviation();

            if (category == StatCategory.ALL) {
                // Add stats for each category with default thresholds
                addAllCategoryStats(stats, player, timePeriod, opponent);
            } else {
                // Add stats for specific category and threshold
                Map<String, Object> statMap = calculateHitRate(
                        player, category, threshold, timePeriod);
                if (statMap != null) {
                    stats.add(dashboardMapper.toStatsRow(
                            player, statMap, category, threshold, opponent));
                }
            }
        }

        // Sort the results
        sortStats(stats, sortBy, sortDirection);
        return stats;
    }

    private List<Players> getTodaysPlayers(List<Games> todaysGames) {
        Set<Long> teamIds = todaysGames.stream()
                .flatMap(game -> Stream.of(
                        game.getHomeTeam().getId(),
                        game.getAwayTeam().getId()))
                .collect(Collectors.toSet());

        return playersRepository.findByTeamIdInAndStatus(teamIds, PlayerStatus.ACTIVE);
    }

    private void addAllCategoryStats(
            List<DashboardStatsRow> stats,
            Players player,
            TimePeriod timePeriod,
            String opponent
    ) {
        // Points
        Map<String, Object> pointStats = calculateHitRate(
                player, StatCategory.POINTS, StatCategory.POINTS.getDefaultThreshold(), timePeriod);
        if (pointStats != null) {
            stats.add(dashboardMapper.toStatsRow(
                    player, pointStats, StatCategory.POINTS, StatCategory.POINTS.getDefaultThreshold(), opponent));
        }

        // Assists
        Map<String, Object> assistStats = calculateHitRate(
                player, StatCategory.ASSISTS, StatCategory.ASSISTS.getDefaultThreshold(), timePeriod);
        if (assistStats != null) {
            stats.add(dashboardMapper.toStatsRow(
                    player, assistStats, StatCategory.ASSISTS, StatCategory.ASSISTS.getDefaultThreshold(), opponent));
        }

        // Rebounds
        Map<String, Object> reboundStats = calculateHitRate(
                player, StatCategory.REBOUNDS, StatCategory.REBOUNDS.getDefaultThreshold(), timePeriod);
        if (reboundStats != null) {
            stats.add(dashboardMapper.toStatsRow(
                    player, reboundStats, StatCategory.REBOUNDS, StatCategory.REBOUNDS.getDefaultThreshold(), opponent));
        }
    }

    private List<GameStats> getPlayerGames(Players player, TimePeriod timePeriod) {
        logger.debug("\n=== Getting Player Games ===");
        logger.debug("Player ID: {}", player.getId());
        logger.debug("Time Period: {}", timePeriod);

        // Try to get from cache first
        List<GameStats> cachedStats = cacheService.getPlayerStats(player, timePeriod);
        if (cachedStats != null) {
            logger.debug("Cache hit - Returning {} games from cache", cachedStats.size());
            return cachedStats;
        }
        logger.debug("Cache miss - Proceeding to repository");

        // If not in cache, get from repository
        int gamesNeeded = getRequiredGamesForPeriod(timePeriod);
        logger.debug("Games needed for period {}: {}", timePeriod, gamesNeeded);

        List<GameStats> repoGames = gameStatsRepository.findPlayerRecentGames(player);

        if (!repoGames.isEmpty()) {
            for (GameStats game : repoGames) {
                logger.info("Game in history: {} - {}@{} on {}",
                        player.getLastName(),
                        game.getGame().getAwayTeam().getAbbreviation(),
                        game.getGame().getHomeTeam().getAbbreviation(),
                        game.getGame().getGameDate());
            }
        }

        logger.debug("Repository returned {} games", repoGames.size());
        if (!repoGames.isEmpty()) {
            logger.debug("First game points from repo: {}", repoGames.get(0).getPoints());
        }

        // Apply limit
        List<GameStats> games = repoGames.stream()
                .limit(gamesNeeded)
                .collect(Collectors.toList());

        logger.debug("After applying limit: {} games", games.size());
        if (!games.isEmpty()) {
            logger.debug("First game points after limit: {}", games.get(0).getPoints());
        }

        return games;
    }

    @Override
    public PlayerDetailStats getPlayerDetailStats(Long playerId, TimePeriod timePeriod,
                                                  StatCategory category, Integer threshold) {
        logger.info("Fetching player detail stats - id: {}, period: {}, category: {}, threshold: {}",
                playerId, timePeriod, category, threshold);

        // Get player or throw exception
        Players player = playersRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found with id: " + playerId));

        // Get all games for the period
        List<GameStats> games = getPlayerGames(player, timePeriod);

        // Calculate hit rates and stats
        Map<String, Object> statsSummary = calculateHitRate(player, category, threshold, timePeriod);

        // Map to DTO using the enhanced mapper
        return playerMapper.toPlayerDetailStats(
                player,
                games,
                statsSummary,
                category,
                timePeriod,
                threshold
        );
    }

    private int getRequiredGamesForPeriod(TimePeriod period) {
        return switch (period) {
            case L5 -> 5;
            case L10 -> 10;
            case L15 -> 15;
            case L20 -> 20;
            case SEASON -> Integer.MAX_VALUE;
        };
    }

    private void sortStats(List<DashboardStatsRow> stats, String sortBy, String sortDirection) {
        Comparator<DashboardStatsRow> comparator = "average".equals(sortBy.toLowerCase()) 
            ? Comparator.comparing(DashboardStatsRow::average)
            : Comparator.comparing(DashboardStatsRow::hitRate);

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        stats.sort(comparator);
    }
}