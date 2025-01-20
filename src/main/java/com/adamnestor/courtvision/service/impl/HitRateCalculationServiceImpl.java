package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.stats.StatsSummary;
import com.adamnestor.courtvision.mapper.DashboardMapper;
import com.adamnestor.courtvision.mapper.PlayerMapper;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.HitRateCalculationService;
import com.adamnestor.courtvision.service.util.DateUtils;
import com.adamnestor.courtvision.service.util.StatAnalysisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class HitRateCalculationServiceImpl implements HitRateCalculationService {
    private static final Logger logger = LoggerFactory.getLogger(HitRateCalculationServiceImpl.class);

    private final GameStatsRepository gameStatsRepository;
    private final GamesRepository gamesRepository;
    private final PlayersRepository playersRepository;
    private final DashboardMapper dashboardMapper;
    private final PlayerMapper playerMapper;
    private final DateUtils dateUtils;

    public HitRateCalculationServiceImpl(
            GameStatsRepository gameStatsRepository,
            GamesRepository gamesRepository,
            PlayersRepository playersRepository,
            DashboardMapper dashboardMapper,
            PlayerMapper playerMapper,
            DateUtils dateUtils) {
        this.gameStatsRepository = gameStatsRepository;
        this.gamesRepository = gamesRepository;
        this.playersRepository = playersRepository;
        this.dashboardMapper = dashboardMapper;
        this.playerMapper = playerMapper;
        this.dateUtils = dateUtils;
    }

    @Override
    @Cacheable(value = "hitRates", 
        key = "#player?.id + ':' + #category + ':' + #threshold + ':' + #period",
        condition = "#player != null")
    public Map<String, Object> calculateHitRate(Players player, StatCategory category, Integer threshold, TimePeriod period) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null.");
        }
        if (category == null) {
            throw new IllegalArgumentException("StatCategory cannot be null.");
        }
        if (period == null) {
            throw new IllegalArgumentException("Time period cannot be null.");
        }
        if (threshold == null || threshold <= 0 || threshold > 51) {
            throw new IllegalArgumentException("Threshold must be a non-negative value but less than 51.");
        }

        logger.info("Calculating hit rate for player {} - {} {} for period {}",
                player.getId(), category, threshold, period);

        // Get player games - no need to check cache, @Cacheable handles it
        List<GameStats> games = getPlayerGames(player, period);
        
        if (games.isEmpty()) {
            return Collections.emptyMap();
        }

        // Calculate hit rate and create result map
        Map<String, Object> result = new HashMap<>();
        result.put("hitRate", calculateHitRateValue(games, category, threshold));
        result.put("average", calculateAverageValue(games, category));
        return result;
    }

    @Cacheable(value = "playerStats",
        key = "#player.id + ':' + #timePeriod")
    private List<GameStats> getPlayerGames(Players player, TimePeriod timePeriod) {
        logger.debug("Cache miss - Fetching player games from repository");
        
        int gamesNeeded = getRequiredGamesForPeriod(timePeriod);
        List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player)
            .stream()
            .limit(gamesNeeded)
            .collect(Collectors.toList());

        logger.debug("Retrieved {} games for player {}", games.size(), player.getId());
        return games;
    }

    @Override
    @Cacheable(value = "confidenceScores",
        key = "#playerId + ':' + #timePeriod + ':' + #category + ':' + #threshold")
    public PlayerDetailStats getPlayerDetailStats(
            Long playerId, TimePeriod timePeriod, StatCategory category, Integer threshold) {
        
        Players player = playersRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        List<GameStats> games = gameStatsRepository.findPlayerRecentGames(player);
        Map<String, Object> stats = calculateStats(games, category, threshold);
        
        StatsSummary summary = new StatsSummary(
            category,
            threshold,
            timePeriod,
            (BigDecimal) stats.get("hitRate"),
            (BigDecimal) stats.get("average"),
            (Integer) stats.get("successCount"),
            (Integer) stats.get("confidenceScore")
        );

        return playerMapper.toPlayerDetailStats(player, summary, threshold);
    }

    private int getStatValue(GameStats game, StatCategory category) {
        return switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
            default -> throw new IllegalArgumentException("Unsupported stat category: " + category);
        };
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
                "scheduled"
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
                Map<String, Object> statMap = createStatMap(player, category, threshold, timePeriod);
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
        Map<String, Object> pointStats = createStatMap(
                player, StatCategory.POINTS, StatCategory.POINTS.getDefaultThreshold(), timePeriod);
        if (pointStats != null) {
            stats.add(dashboardMapper.toStatsRow(
                    player, pointStats, StatCategory.POINTS, StatCategory.POINTS.getDefaultThreshold(), opponent));
        }

        // Assists
        Map<String, Object> assistStats = createStatMap(
                player, StatCategory.ASSISTS, StatCategory.ASSISTS.getDefaultThreshold(), timePeriod);
        if (assistStats != null) {
            stats.add(dashboardMapper.toStatsRow(
                    player, assistStats, StatCategory.ASSISTS, StatCategory.ASSISTS.getDefaultThreshold(), opponent));
        }

        // Rebounds
        Map<String, Object> reboundStats = createStatMap(
                player, StatCategory.REBOUNDS, StatCategory.REBOUNDS.getDefaultThreshold(), timePeriod);
        if (reboundStats != null) {
            stats.add(dashboardMapper.toStatsRow(
                    player, reboundStats, StatCategory.REBOUNDS, StatCategory.REBOUNDS.getDefaultThreshold(), opponent));
        }
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

    private Map<String, Object> createStatMap(Players player, StatCategory category, 
                                            Integer threshold, TimePeriod timePeriod) {
        Map<String, Object> hitRateResult = calculateHitRate(player, category, threshold, timePeriod);
        Map<String, Object> statMap = new HashMap<>();
        statMap.put("hitRate", hitRateResult.get("hitRate"));
        statMap.put("category", category);
        statMap.put("threshold", threshold);
        return statMap;
    }

    // Helper methods for calculations
    private BigDecimal calculateHitRateValue(List<GameStats> games, StatCategory category, Integer threshold) {
        long hits = games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count();
        
        return BigDecimal.valueOf(hits)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(games.size()), java.math.MathContext.DECIMAL32)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageValue(List<GameStats> games, StatCategory category) {
        return games.stream()
                .map(game -> BigDecimal.valueOf(getStatValue(game, category)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(games.size()), java.math.MathContext.DECIMAL32)
                .setScale(4, RoundingMode.HALF_UP);
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

    private Map<String, Object> calculateStats(List<GameStats> games, StatCategory category, Integer threshold) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", calculateHitRateValue(games, category, threshold));
        stats.put("average", calculateAverageValue(games, category));
        stats.put("successCount", games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count());
        stats.put("confidenceScore", calculateConfidenceScore(games, category, threshold));
        return stats;
    }

    private int calculateConfidenceScore(List<GameStats> games, StatCategory category, Integer threshold) {
        BigDecimal hitRate = calculateHitRateValue(games, category, threshold);
        return hitRate.multiply(BigDecimal.valueOf(0.8))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }
}