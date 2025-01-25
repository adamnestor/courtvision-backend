package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import com.adamnestor.courtvision.dto.stats.StatsSummary;
import com.adamnestor.courtvision.mapper.DashboardMapper;
import com.adamnestor.courtvision.mapper.PlayerStatsMapper;
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
    private final PlayerStatsMapper playerStatsMapper;
    private final DateUtils dateUtils;

    public HitRateCalculationServiceImpl(
            GameStatsRepository gameStatsRepository,
            GamesRepository gamesRepository,
            PlayersRepository playersRepository,
            DashboardMapper dashboardMapper,
            PlayerStatsMapper playerStatsMapper,
            DateUtils dateUtils) {
        this.gameStatsRepository = gameStatsRepository;
        this.gamesRepository = gamesRepository;
        this.playersRepository = playersRepository;
        this.dashboardMapper = dashboardMapper;
        this.playerStatsMapper = playerStatsMapper;
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
        result.put("confidenceScore", calculateConfidenceScore(games, category, threshold));
        return result;
    }

    @Cacheable(value = "playerGames", 
        key = "#player.id + ':' + #timePeriod",
        unless = "#result.isEmpty()")
    public List<GameStats> getPlayerGames(Players player, TimePeriod timePeriod) {
        logger.debug("Cache miss - Fetching player games from repository");
        
        int gamesNeeded = getRequiredGamesForPeriod(timePeriod);
        return gameStatsRepository.findPlayerRecentGames(player)
            .stream()
            .limit(gamesNeeded)
            .collect(Collectors.toList());
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

        return playerStatsMapper.toPlayerDetailStats(player, summary, threshold);
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
    @Deprecated
    public List<DashboardStatsResponse> getDashboardStats(
        TimePeriod timePeriod,
        StatCategory category,
        Integer threshold,
        String sortBy,
        String sortDirection
    ) {
        // Call the new method with a temporary mapper
        return getDashboardStats(
            timePeriod,
            category,
            threshold,
            sortBy,
            new DashboardMapper(),
            sortDirection
        );
    }

    @Override
    @Cacheable(value = "dashboardStats",
        key = "#timePeriod + ':' + #category + ':' + #threshold + ':' + #sortBy + ':' + #sortDirection",
        unless = "#result.isEmpty()")
    public List<DashboardStatsResponse> getDashboardStats(
        TimePeriod timePeriod,
        StatCategory category,
        Integer threshold,
        String sortBy,
        DashboardMapper dashboardMapper,
        String sortDirection
    ) {
        logger.info("Getting dashboard stats. Games exist for today: {}", !gamesRepository.findByGameDateAndStatus(
            dateUtils.getCurrentEasternDate(), "scheduled").isEmpty());

        // Get today's games
        List<Games> todaysGames = gamesRepository.findByGameDateAndStatus(
            dateUtils.getCurrentEasternDate(),
            "scheduled"
        );
        
        logger.info("Found {} games for today", todaysGames.size());
        
        if (todaysGames.isEmpty()) {
            logger.warn("No games found for today, returning empty list");
            return Collections.emptyList();
        }
        
        // Get teams playing today
        Set<Long> teamsWithGames = todaysGames.stream()
            .flatMap(game -> Stream.of(game.getHomeTeam().getId(), game.getAwayTeam().getId()))
            .collect(Collectors.toSet());
            
        logger.info("Found {} teams with games today", teamsWithGames.size());

        List<DashboardStatsResponse> stats = new ArrayList<>();

        for (Players player : getTodaysPlayers(todaysGames)) {
            // Find this player's game for today
            Games playerGame = todaysGames.stream()
                    .filter(game -> 
                        game.getHomeTeam().getId().equals(player.getTeam().getId())
                        || game.getAwayTeam().getId().equals(player.getTeam().getId())
                    )
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Player game not found"));

            // Calculate opponent string (vs LAL or @ LAL)
            String opponent = playerGame.getHomeTeam().equals(player.getTeam())
                    ? "vs " + playerGame.getAwayTeam().getAbbreviation()
                    : "@ " + playerGame.getHomeTeam().getAbbreviation();

            boolean isAway = !playerGame.getHomeTeam().equals(player.getTeam());

            // Add stats for specific category and threshold
            Map<String, Object> statMap = createStatMap(player, category, threshold, timePeriod);
            if (statMap != null) {
                stats.add(dashboardMapper.toStatsResponse(
                        player, playerGame, statMap, opponent, isAway));
            }
        }

        // Sort the results
        sortStats(stats, sortBy, sortDirection);
        return stats;
    }

    @Cacheable(value = "todaysPlayers",
        key = "#todaysGames.hashCode()",
        unless = "#result.isEmpty()")
    private List<Players> getTodaysPlayers(List<Games> todaysGames) {
        Set<Long> teamIds = todaysGames.stream()
                .flatMap(game -> Stream.of(
                        game.getHomeTeam().getId(),
                        game.getAwayTeam().getId()))
                .collect(Collectors.toSet());

        return playersRepository.findByTeamIdInAndStatus(teamIds, PlayerStatus.ACTIVE);
    }

    private void sortStats(List<DashboardStatsResponse> stats, String sortBy, String sortDirection) {
        Comparator<DashboardStatsResponse> comparator = "average".equals(sortBy.toLowerCase()) 
            ? Comparator.comparing(DashboardStatsResponse::average, Comparator.nullsLast(BigDecimal::compareTo))
            : Comparator.comparing(DashboardStatsResponse::hitRate, Comparator.nullsLast(BigDecimal::compareTo));

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        stats.sort(comparator);
    }

    @Cacheable(value = "statMaps",
        key = "#player.id + ':' + #category + ':' + #threshold + ':' + #timePeriod",
        unless = "#result == null")
    private Map<String, Object> createStatMap(
        Players player,
        StatCategory category,
        Integer threshold,
        TimePeriod timePeriod
    ) {
        Map<String, Object> hitRateResult = calculateHitRate(player, category, threshold, timePeriod);
        Map<String, Object> statMap = new HashMap<>();
        
        // Add logging to verify values
        logger.info("Player: {}, Category: {}, Threshold: {}", 
            player.getLastName(), category, threshold);
        logger.info("Hit Rate: {}, Confidence Score: {}", 
            hitRateResult.get("hitRate"), hitRateResult.get("confidenceScore"));
        
        // Ensure we're passing the exact values to the mapper
        statMap.putAll(hitRateResult);
        statMap.put("category", category);
        statMap.put("threshold", threshold);
        
        return statMap;
    }

    // Helper methods for calculations
    private BigDecimal calculateHitRateValue(List<GameStats> games, StatCategory category, Integer threshold) {
        if (games.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        long hits = games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count();
        
        return BigDecimal.valueOf(hits)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(games.size()), java.math.MathContext.DECIMAL32)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageValue(List<GameStats> games, StatCategory category) {
        if (games.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
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

    @Cacheable(value = "calculatedStats",
        key = "#category + ':' + #threshold + ':' + #games.hashCode()",
        unless = "#result.isEmpty()")
    public Map<String, Object> calculateStats(
        List<GameStats> games,
        StatCategory category,
        Integer threshold
    ) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", calculateHitRateValue(games, category, threshold));
        stats.put("average", calculateAverageValue(games, category));
        stats.put("successCount", Integer.valueOf((int) games.stream()
                .filter(game -> getStatValue(game, category) >= threshold)
                .count()));
        stats.put("confidenceScore", Integer.valueOf(calculateConfidenceScore(games, category, threshold)));
        return stats;
    }

    private int calculateConfidenceScore(List<GameStats> games, StatCategory category, Integer threshold) {
        BigDecimal hitRate = calculateHitRateValue(games, category, threshold);
        return hitRate.multiply(BigDecimal.valueOf(0.8))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    @Override
    public List<DashboardStatsResponse> calculateDashboardStats(
        String timeFrame,
        StatCategory category,
        Integer threshold,
        DashboardMapper dashboardMapper
    ) {
        logger.debug("Calculating dashboard stats - timeFrame: {}, category: {}, threshold: {}", 
            timeFrame, category, threshold);
        
        TimePeriod period = timeFrame != null ? TimePeriod.valueOf(timeFrame) : TimePeriod.L5;
        
        return getDashboardStats(
            period,
            category,
            threshold,
            "hitrate",
            dashboardMapper,
            "desc"
        );
    }
}