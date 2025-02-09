package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.confidence.service.ConfidenceScoreService;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import com.adamnestor.courtvision.dto.response.GameStatDetail;
import com.adamnestor.courtvision.mapper.DashboardMapper;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.HitRateCalculationService;
import com.adamnestor.courtvision.service.util.DateUtils;
import com.adamnestor.courtvision.service.util.StatAnalysisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final DateUtils dateUtils;
    private final ConfidenceScoreService confidenceScoreService;

    public HitRateCalculationServiceImpl(
            GameStatsRepository gameStatsRepository,
            GamesRepository gamesRepository,
            PlayersRepository playersRepository,
            DashboardMapper dashboardMapper,
            DateUtils dateUtils,
            ConfidenceScoreService confidenceScoreService) {
        this.gameStatsRepository = gameStatsRepository;
        this.gamesRepository = gamesRepository;
        this.playersRepository = playersRepository;
        this.dashboardMapper = dashboardMapper;
        this.dateUtils = dateUtils;
        this.confidenceScoreService = confidenceScoreService;
    }

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

        // Get player games
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

    public List<GameStats> getPlayerGames(Players player, TimePeriod timePeriod) {
        logger.debug("Fetching player games from repository");
        
        int gamesNeeded = getRequiredGamesForPeriod(timePeriod);
        return gameStatsRepository.findPlayerRecentGames(player)
            .stream()
            .limit(gamesNeeded)
            .collect(Collectors.toList());
    }

    public PlayerDetailStats getPlayerDetailStats(
            Long playerId,
            TimePeriod timePeriod,
            StatCategory category,
            Integer threshold) {
        
        Players player = playersRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        List<GameStats> games = getPlayerGames(player, timePeriod);
        Map<String, Object> stats = calculateStats(games, category, threshold);
        
        List<GameStatDetail> gameDetails = games.stream()
            .map(game -> new GameStatDetail(
                game.getGame().getGameDate().toString(),
                getOpponentString(game.getGame(), player.getTeam()),
                !game.getGame().getHomeTeam().getId().equals(player.getTeam().getId()),
                getStatValue(game, category),
                getStatValue(game, category) >= threshold
            ))
            .collect(Collectors.toList());

        return new PlayerDetailStats(
            player.getId(),
            player.getFirstName() + " " + player.getLastName(),
            player.getTeam().getAbbreviation(),
            (BigDecimal) stats.get("hitRate"),
            (Integer) stats.get("confidenceScore"),
            games.size(),
            (BigDecimal) stats.get("average"),
            gameDetails
        );
    }

    private String getOpponentString(Games game, Teams playerTeam) {
        if (game.getHomeTeam().getId().equals(playerTeam.getId())) {
            return "vs " + game.getAwayTeam().getAbbreviation();
        } else {
            return "@ " + game.getHomeTeam().getAbbreviation();
        }
    }

    private int getStatValue(GameStats game, StatCategory category) {
        return switch (category) {
            case POINTS -> game.getPoints();
            case ASSISTS -> game.getAssists();
            case REBOUNDS -> game.getRebounds();
            default -> throw new IllegalArgumentException("Unsupported stat category: " + category);
        };
    }

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

    public boolean hasSufficientData(Players player, TimePeriod timePeriod) {
        List<GameStats> games = getPlayerGames(player, timePeriod);
        int requiredGames = getRequiredGamesForPeriod(timePeriod);

        boolean sufficient = games.size() >= requiredGames;
        logger.debug("Player {} has {} games, required {}", player.getId(),
                games.size(), requiredGames);

        return sufficient;
    }

    @Deprecated
    public List<DashboardStatsResponse> getDashboardStats(
        TimePeriod timePeriod,
        StatCategory category,
        Integer threshold,
        String sortBy,
        String sortDirection
    ) {
        // Call the new method with the injected mapper
        return getDashboardStats(
            timePeriod,
            category,
            threshold,
            sortBy,
            dashboardMapper,
            sortDirection
        );
    }

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

        List<Games> todaysGames = gamesRepository.findByGameDateAndStatus(
            dateUtils.getCurrentEasternDate(), "scheduled");
        
        List<Players> todaysPlayers = getTodaysPlayers(todaysGames);

        // Step 1: Calculate hit rates for ALL players
        List<PlayerStats> allPlayers = todaysPlayers.parallelStream()
            .map(player -> {
                List<GameStats> games = getPlayerGames(player, timePeriod);
                if (games.isEmpty()) return null;

                BigDecimal hitRate = calculateHitRateValue(games, category, threshold);
                if (hitRate == null) return null;

                Map<String, Object> stats = new HashMap<>();
                stats.put("hitRate", hitRate);
                stats.put("average", calculateAverageValue(games, category));
                stats.put("category", category);
                stats.put("threshold", threshold);
                
                // Only calculate confidence score if hit rate ≥ 60%
                if (hitRate.compareTo(new BigDecimal("60.0")) >= 0) {
                    Games game = todaysGames.stream()
                        .filter(g -> g.getHomeTeam().getId().equals(player.getTeam().getId()) || 
                                   g.getAwayTeam().getId().equals(player.getTeam().getId()))
                        .findFirst()
                        .orElse(null);
                    
                    if (game != null) {
                        int gamesCount = getRequiredGamesForPeriod(timePeriod);
                        BigDecimal confidence = confidenceScoreService.calculateConfidenceScore(
                            player, game, category, threshold, hitRate, gamesCount
                        );
                        stats.put("confidenceScore", confidence.intValue());
                    }
                }

                return new PlayerStats(player, stats, games);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // Step 2: Only return players with hit rate ≥ 60% to frontend
        return allPlayers.parallelStream()
            // Filter 1: Must have hit rate ≥ 60%
            .filter(ps -> {
                BigDecimal hitRate = (BigDecimal) ps.stats().get("hitRate");
                return hitRate.compareTo(new BigDecimal("60.0")) >= 0;
            })
            .map(ps -> {
                Games game = todaysGames.stream()
                    .filter(g -> g.getHomeTeam().getId().equals(ps.player().getTeam().getId()) || 
                               g.getAwayTeam().getId().equals(ps.player().getTeam().getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Game not found for player with game today"));

                boolean isAway = !game.getHomeTeam().getId().equals(ps.player().getTeam().getId());
                String opponent = isAway ? 
                    "@ " + game.getHomeTeam().getAbbreviation() : 
                    "vs " + game.getAwayTeam().getAbbreviation();

                return dashboardMapper.toStatsResponse(
                    ps.player(), game, ps.stats(), opponent, isAway);
            })
            .sorted(createComparator(sortBy, sortDirection))
            .collect(Collectors.toList());
    }

    private record PlayerStats(Players player, Map<String, Object> stats, List<GameStats> games) {}

    private List<Players> getTodaysPlayers(List<Games> todaysGames) {
        Set<Long> teamIds = todaysGames.stream()
                .flatMap(game -> Stream.of(
                        game.getHomeTeam().getId(),
                        game.getAwayTeam().getId()))
                .collect(Collectors.toSet());

        return playersRepository.findByTeamIdInAndStatus(teamIds, PlayerStatus.ACTIVE);
    }

    private Comparator<DashboardStatsResponse> createComparator(String sortBy, String sortDirection) {
        Comparator<DashboardStatsResponse> comparator = "average".equals(sortBy.toLowerCase()) 
            ? Comparator.comparing(DashboardStatsResponse::average, Comparator.nullsLast(BigDecimal::compareTo))
            : Comparator.comparing(DashboardStatsResponse::hitRate, Comparator.nullsLast(BigDecimal::compareTo));

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        return comparator;
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

        // Only calculate confidence score if hit rate ≥ 60%
        if (hitRate.compareTo(new BigDecimal("60.0")) < 0) {
            return 0;
        }

        // Get the player and game from the most recent GameStats
        GameStats latestGame = games.get(0); // games are already ordered by date desc
        Players player = latestGame.getPlayer();
        Games game = latestGame.getGame();

        // Calculate confidence score using our new service
        return confidenceScoreService.calculateConfidenceScore(
                player,
                game,
                category,
                threshold,
                hitRate,
                games.size()
        ).intValue();
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