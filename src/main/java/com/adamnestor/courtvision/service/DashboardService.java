package com.adamnestor.courtvision.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayList;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.dto.DashboardResponse;
import com.adamnestor.courtvision.dto.DashboardMetadata;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.util.DateUtils;
import com.adamnestor.courtvision.dto.PageInfo;

@Service
public class DashboardService {
    private final GamesRepository gamesRepository;
    private final PlayersRepository playersRepository;
    private final StatsService statsService;
    private final DateUtils dateUtils;

    public DashboardService(
            GamesRepository gamesRepository,
            PlayersRepository playersRepository,
            StatsService statsService,
            DateUtils dateUtils) {
        this.gamesRepository = gamesRepository;
        this.playersRepository = playersRepository;
        this.statsService = statsService;
        this.dateUtils = dateUtils;
    }

    public DashboardResponse getDashboardStats(
        int page, 
        int size, 
        String timeFrame, 
        String category,
        String threshold,
        String sortBy,
        String sortDir
    ) {
        LocalDate today = dateUtils.getCurrentEasternDate();
        List<Games> todaysGames = gamesRepository.findByGameDate(today);
        Integer thresholdValue = parseThreshold(threshold);
        
        // 1. Get complete dataset
        Set<Long> uniqueGameIds = new HashSet<>();
        List<DashboardStatsResponse> allStats = todaysGames.stream()
            .flatMap(game -> {
                uniqueGameIds.add(game.getId());
                return Stream.concat(
                    getTeamPlayers(game.getHomeTeam(), game, timeFrame, thresholdValue, category),
                    getTeamPlayers(game.getAwayTeam(), game, timeFrame, thresholdValue, category)
                );
            })
            .collect(Collectors.toList());

        // 2. Apply filters
        List<DashboardStatsResponse> filteredStats = allStats.stream()
            .filter(stat -> filterByCategory(stat, category))
            .filter(stat -> filterByTimeFrame(stat, timeFrame))
            .collect(Collectors.toList());

        // 3. Apply sorting
        List<DashboardStatsResponse> sortedStats = filteredStats.stream()
            .sorted((a, b) -> sortStats(a, b, sortBy, sortDir))
            .collect(Collectors.toList());

        // 4. Apply pagination
        int start = page * size;
        int end = Math.min((start + size), sortedStats.size());
        List<DashboardStatsResponse> pagedStats = 
            start < sortedStats.size() ? sortedStats.subList(start, end) : new ArrayList<>();

        // Create response with metadata
        return new DashboardResponse(
            pagedStats,
            new DashboardMetadata(uniqueGameIds.size()),
            new PageInfo(
                page,
                size,
                (int) Math.ceil(sortedStats.size() / (double) size),
                sortedStats.size()
            )
        );
    }

    private Integer parseThreshold(String threshold) {
        if (threshold == null) return null;
        return Integer.parseInt(threshold.replace("+", ""));
    }

    private boolean filterByCategory(DashboardStatsResponse stat, String category) {
        if (category == null) return true;
        return stat.category().equals(category);
    }

    private boolean filterByTimeFrame(DashboardStatsResponse stat, String timeFrame) {
        // We no longer need to filter by games played at this level
        // The filtering is done when fetching stats in createPlayerDashboardStats
        return true;
    }

    private int sortStats(DashboardStatsResponse a, DashboardStatsResponse b, 
                         String sortBy, String sortDir) {
        int multiplier = sortDir.equalsIgnoreCase("desc") ? -1 : 1;
        
        if (sortBy == null) {
            return multiplier * b.confidenceScore().compareTo(a.confidenceScore());
        }

        return multiplier * switch (sortBy.toLowerCase()) {
            case "hitrate" -> b.hitRate().compareTo(a.hitRate());
            case "confidencescore" -> b.confidenceScore().compareTo(a.confidenceScore());
            default -> b.confidenceScore().compareTo(a.confidenceScore());
        };
    }

    private Stream<DashboardStatsResponse> getTeamPlayers(
        Teams team, 
        Games game, 
        String timeFrame, 
        Integer threshold,
        String category
    ) {
        return playersRepository.findByTeamAndStatus(team, PlayerStatus.ACTIVE)
            .stream()
            .map(player -> {
                List<GameStats> stats = statsService.getFilteredPlayerStats(
                    player, 
                    getNumGamesForTimeFrame(timeFrame),
                    StatCategory.valueOf(category),
                    threshold
                );
                return createPlayerDashboardStats(player, game, stats);
            });
    }

    private int getNumGamesForTimeFrame(String timeFrame) {
        return switch (timeFrame != null ? timeFrame : "L5") {
            case "L5" -> 5;
            case "L10" -> 10;
            case "L15" -> 15;
            case "L20" -> 20;
            case "SEASON" -> Integer.MAX_VALUE;
            default -> 5;
        };
    }

    private DashboardStatsResponse createPlayerDashboardStats(
        Players player, 
        Games game,
        List<GameStats> stats
    ) {
        BigDecimal hitRate = calculateHitRate(stats, null);
        Integer confidenceScore = calculateConfidenceScore(stats);

        return DashboardStatsResponse.create(
            player,
            game,
            StatCategory.POINTS,
            20,
            hitRate,
            confidenceScore
        );
    }

    private BigDecimal calculateHitRate(List<GameStats> stats, Integer threshold) {
        if (stats.isEmpty()) return BigDecimal.ZERO;
        int thresholdValue = threshold != null ? threshold : 20;
        long successCount = stats.stream()
            .filter(stat -> stat.getPoints() >= thresholdValue)
            .count();
        return BigDecimal.valueOf(successCount * 100.0 / stats.size());
    }

    private Integer calculateConfidenceScore(List<GameStats> stats) {
        if (stats.isEmpty()) return 0;
        // Example implementation
        return (int) (calculateHitRate(stats, null).doubleValue() * 0.8);
    }

    // Helper methods for calculations...
} 