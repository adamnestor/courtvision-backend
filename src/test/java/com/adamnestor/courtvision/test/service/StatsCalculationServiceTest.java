package com.adamnestor.courtvision.test.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.dto.player.GamePerformance;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.player.PlayerInfo;
import com.adamnestor.courtvision.dto.stats.StatsSummary;
import com.adamnestor.courtvision.mapper.DashboardMapper;
import com.adamnestor.courtvision.mapper.PlayerMapper;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.StatsCalculationService;
import com.adamnestor.courtvision.service.cache.StatsCacheService;
import com.adamnestor.courtvision.service.impl.StatsCalculationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsCalculationServiceTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private StatsCacheService cacheService;

    @Mock
    private PlayersRepository playersRepository;

    @Mock
    private DashboardMapper dashboardMapper;

    @Mock
    private PlayerMapper playerMapper;

    private StatsCalculationService statsService;
    private Players testPlayer;
    private List<GameStats> testGames;

    @BeforeEach
    void setUp() {
        statsService = new StatsCalculationServiceImpl(gameStatsRepository, cacheService, playersRepository, dashboardMapper, playerMapper);
        testPlayer = createTestPlayer();
        testGames = createTestGames();
    }

    // Hit Rate Tests
    @Test
    void calculateHitRate_CacheHit_ReturnsFromCache() {
        Map<String, Object> expectedStats = Map.of(
                "hitRate", new BigDecimal("80.00"),
                "average", new BigDecimal("22.50"),
                "successCount", 8,
                "failureCount", 2,
                "category", StatCategory.POINTS,
                "threshold", 20
        );
        when(cacheService.getHitRate(any(Players.class), any(StatCategory.class),
                any(Integer.class), any(TimePeriod.class))).thenReturn(expectedStats);

        Map<String, Object> result = statsService.calculateHitRate(
                testPlayer, StatCategory.POINTS, 20, TimePeriod.L10);

        assertThat(result).isEqualTo(expectedStats);
        verify(gameStatsRepository, never()).findPlayerRecentGames(any(Players.class));
    }

    @Test
    void calculateHitRate_CacheMiss_CalculatesFromGames() {
        when(cacheService.getHitRate(any(Players.class), any(StatCategory.class),
                any(Integer.class), any(TimePeriod.class))).thenReturn(null);
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(testGames);

        Map<String, Object> result = statsService.calculateHitRate(
                testPlayer, StatCategory.POINTS, 20, TimePeriod.L10);

        assertThat(result)
                .containsKeys("hitRate", "average", "successCount", "failureCount", "category", "threshold");

        assertThat((BigDecimal) result.get("hitRate"))
                .isEqualByComparingTo("100.00");
        assertThat((BigDecimal) result.get("average"))
                .isEqualByComparingTo("24.50");
        assertThat(result.get("successCount")).isEqualTo(10);
        assertThat(result.get("failureCount")).isEqualTo(0);
    }

    @ParameterizedTest
    @EnumSource(StatCategory.class)
    void calculateHitRate_ForAllCategories(StatCategory category) {
        when(cacheService.getHitRate(any(Players.class), any(StatCategory.class),
                any(Integer.class), any(TimePeriod.class))).thenReturn(null);
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(testGames);

        Map<String, Object> result = statsService.calculateHitRate(
                testPlayer, category, getDefaultThreshold(category), TimePeriod.L10);

        assertThat(result.get("category")).isEqualTo(category);
        assertThat((BigDecimal) result.get("hitRate")).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat((BigDecimal) result.get("average")).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @EnumSource(TimePeriod.class)
    void calculateHitRate_ForAllTimePeriods(TimePeriod period) {
        when(cacheService.getHitRate(any(Players.class), any(StatCategory.class),
                any(Integer.class), any(TimePeriod.class))).thenReturn(null);
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(testGames);

        Map<String, Object> result = statsService.calculateHitRate(
                testPlayer, StatCategory.POINTS, 20, period);

        assertThat(result).containsKeys("hitRate", "average", "successCount", "failureCount");
    }

    @Test
    void calculateHitRate_WithEmptyGamesList() {
        when(cacheService.getHitRate(any(Players.class), any(StatCategory.class),
                any(Integer.class), any(TimePeriod.class))).thenReturn(null);
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class)))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = statsService.calculateHitRate(
                testPlayer, StatCategory.POINTS, 20, TimePeriod.L10);

        assertThat((BigDecimal) result.get("hitRate"))
                .isEqualByComparingTo("0.00");
        assertThat((BigDecimal) result.get("average"))
                .isEqualByComparingTo("0.00");
        assertThat(result.get("successCount")).isEqualTo(0);
        assertThat(result.get("failureCount")).isEqualTo(0);
    }

    @Test
    void calculateHitRate_WithNullGamesList() {
        when(cacheService.getHitRate(any(Players.class), any(StatCategory.class),
                any(Integer.class), any(TimePeriod.class))).thenReturn(null);
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(null);

        Map<String, Object> result = statsService.calculateHitRate(
                testPlayer, StatCategory.POINTS, 20, TimePeriod.L10);

        assertThat((BigDecimal) result.get("hitRate"))
                .isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1000})
    void calculateHitRate_WithInvalidThresholds(int threshold) {
        assertThatThrownBy(() ->
                statsService.calculateHitRate(testPlayer, StatCategory.POINTS, threshold, TimePeriod.L10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Average Tests
    @Test
    void getPlayerAverages_ReturnsCorrectAverages() {
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(testGames);

        Map<StatCategory, BigDecimal> averages = statsService.getPlayerAverages(testPlayer, TimePeriod.L10);

        assertThat(averages)
                .containsKeys(StatCategory.POINTS, StatCategory.ASSISTS, StatCategory.REBOUNDS);
        assertThat(averages.get(StatCategory.POINTS))
                .isEqualByComparingTo("24.50");
        assertThat(averages.values())
                .allMatch(avg -> avg.scale() <= 2);
    }

    @Test
    void getPlayerAverages_WithEmptyGamesList() {
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class)))
                .thenReturn(Collections.emptyList());

        Map<StatCategory, BigDecimal> averages = statsService.getPlayerAverages(testPlayer, TimePeriod.L10);

        Arrays.stream(StatCategory.values())
                .forEach(category ->
                        assertThat(averages.get(category))
                                .isEqualByComparingTo("0.00"));
    }

    @ParameterizedTest
    @EnumSource(TimePeriod.class)
    void getPlayerAverages_ForAllTimePeriods(TimePeriod period) {
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(testGames);

        Map<StatCategory, BigDecimal> averages = statsService.getPlayerAverages(testPlayer, period);

        assertThat(averages).containsKeys(StatCategory.values());
        assertThat(averages.values()).allMatch(avg -> avg.scale() <= 2);
    }

    // Data Sufficiency Tests
    @ParameterizedTest
    @EnumSource(TimePeriod.class)
    void hasSufficientData_ForAllTimePeriods(TimePeriod period) {
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(testGames);

        boolean result = statsService.hasSufficientData(testPlayer, period);

        int requiredGames = switch (period) {
            case L5 -> 5;
            case L10 -> 10;
            case L15 -> 15;
            case L20 -> 20;
            case SEASON -> Integer.MAX_VALUE;
        };

        assertThat(result).isEqualTo(testGames.size() >= requiredGames);
    }

    @Test
    void hasSufficientData_WithNullGamesList() {
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(null);

        boolean result = statsService.hasSufficientData(testPlayer, TimePeriod.L10);

        assertThat(result).isFalse();
    }

    // Cache Tests
    @Test
    void calculateHitRate_WithPartialCache() {
        List<GameStats> cachedGames = testGames.subList(0, 5);
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(cachedGames);
        when(cacheService.getHitRate(any(Players.class), any(StatCategory.class),
                any(Integer.class), any(TimePeriod.class))).thenReturn(null);

        Map<String, Object> result = statsService.calculateHitRate(
                testPlayer, StatCategory.POINTS, 20, TimePeriod.L10);

        verify(gameStatsRepository, never()).findPlayerRecentGames(any(Players.class));
        assertThat((BigDecimal) result.get("hitRate"))
                .isEqualByComparingTo("100.00");
    }

    // Null Validation Tests
    @Test
    void calculateHitRate_WithNullPlayer() {
        assertThatThrownBy(() ->
                statsService.calculateHitRate(null, StatCategory.POINTS, 20, TimePeriod.L10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player cannot be null");
    }

    @Test
    void calculateHitRate_WithNullCategory() {
        assertThatThrownBy(() ->
                statsService.calculateHitRate(testPlayer, null, 20, TimePeriod.L10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category cannot be null");
    }

    @Test
    void calculateHitRate_WithNullTimePeriod() {
        assertThatThrownBy(() ->
                statsService.calculateHitRate(testPlayer, StatCategory.POINTS, 20, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Time period cannot be null");
    }

    @Test
    void getPlayerAverages_WithInvalidTimePeriod() {
        assertThatThrownBy(() ->
                statsService.getPlayerAverages(testPlayer, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Time period cannot be null");
    }

    @Test
    void getPlayerAverages_WithNullPlayer() {
        assertThatThrownBy(() ->
                statsService.getPlayerAverages(null, TimePeriod.L10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player cannot be null");
    }

    @Test
    void getDashboardStats_ReturnsFilteredAndSortedList() {
        // Arrange
        List<Players> testPlayers = List.of(createTestPlayer());
        List<GameStats> games = createTestGames();

        // Mock repository calls
        when(playersRepository.findByStatus(PlayerStatus.ACTIVE)).thenReturn(testPlayers);

        // Mock cache behavior
        when(cacheService.getPlayerStats(any(), any())).thenReturn(games);
        Map<String, Object> hitRateMap = Map.of(
                "hitRate", new BigDecimal("80.00"),
                "average", new BigDecimal("22.50"),
                "successCount", 8,
                "failureCount", 2,
                "threshold", 20
        );
        when(cacheService.getHitRate(any(), any(), any(), any())).thenReturn(hitRateMap);

        // Mock mapper
        DashboardStatsRow mappedRow = new DashboardStatsRow(
                1L, "Test Player", "DEN", StatCategory.POINTS, 20, TimePeriod.L10,
                new BigDecimal("80.00"), new BigDecimal("22.50"), 10
        );
        when(dashboardMapper.toStatsRow(any(), any(), any(), any()))
                .thenReturn(mappedRow);

        // Act
        List<DashboardStatsRow> result = statsService.getDashboardStats(
                TimePeriod.L10, StatCategory.POINTS, 20, "hitRate", "desc"
        );

        // Assert
        assertThat(result)
                .isNotEmpty()
                .allSatisfy(row -> {
                    assertThat(row.hitRate()).isEqualByComparingTo("80.00");
                    assertThat(row.average()).isEqualByComparingTo("22.50");
                    assertThat(row.gamesAnalyzed()).isEqualTo(10);
                });
    }

    @Test
    void getPlayerDetailStats_ReturnsCompletePlayerStats() {
        // Arrange
        Players player = createTestPlayer();
        List<GameStats> games = createTestGames();

        // Mock repository and cache
        when(playersRepository.findById(1L)).thenReturn(Optional.of(player));
        when(cacheService.getPlayerStats(any(), any())).thenReturn(games);
        Map<String, Object> hitRateMap = Map.of(
                "hitRate", new BigDecimal("80.00"),
                "average", new BigDecimal("22.50"),
                "successCount", 8,
                "failureCount", 2,
                "threshold", 20
        );
        when(cacheService.getHitRate(any(), any(), any(), any())).thenReturn(hitRateMap);

        PlayerDetailStats expectedStats = new PlayerDetailStats(
                new PlayerInfo(1L, "Test", "Player", "DEN", "F"),
                List.of(new GamePerformance(1L, LocalDate.now(), "OPP", true, 22, 5, 8, "32:00", "100-95")),
                new StatsSummary(StatCategory.POINTS, 20, TimePeriod.L10,
                        new BigDecimal("80.00"), new BigDecimal("22.50"), 8, 2)
        );
        when(playerMapper.toPlayerDetailStats(any(), any(), any(), any(), any()))
                .thenReturn(expectedStats);

        // Act
        PlayerDetailStats result = statsService.getPlayerDetailStats(1L, TimePeriod.L10,
                StatCategory.POINTS, 20);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(stats -> {
                    assertThat(stats.player().playerId()).isEqualTo(1L);
                    assertThat(stats.games()).isNotEmpty();
                    assertThat(stats.summary()).isNotNull();
                });
    }

    // Helper Methods
    private Players createTestPlayer() {
        Players player = new Players();
        player.setId(1L);
        player.setFirstName("Test");
        player.setLastName("Player");
        return player;
    }

    private List<GameStats> createTestGames() {
        List<GameStats> games = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GameStats game = new GameStats();
            game.setPoints(20 + i);     // 20-29 points
            game.setAssists(5 + (i % 3)); // 5-7 assists
            game.setRebounds(8 + (i % 4)); // 8-11 rebounds
            games.add(game);
        }
        return games;
    }

    private int getDefaultThreshold(StatCategory category) {
        return switch (category) {
            case POINTS -> 20;
            case ASSISTS -> 5;
            case REBOUNDS -> 8;
        };
    }
}