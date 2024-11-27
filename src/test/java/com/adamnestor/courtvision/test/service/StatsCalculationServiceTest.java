package com.adamnestor.courtvision.test.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.service.StatsCalculationService;
import com.adamnestor.courtvision.service.cache.StatsCacheService;
import com.adamnestor.courtvision.service.impl.StatsCalculationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsCalculationServiceTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private StatsCacheService cacheService;

    private StatsCalculationService statsService;
    private Players testPlayer;
    private List<GameStats> testGames;

    @BeforeEach
    void setUp() {
        statsService = new StatsCalculationServiceImpl(gameStatsRepository, cacheService);
        testPlayer = createTestPlayer();
        testGames = createTestGames();
    }

    @Test
    void calculateHitRate_CacheHit_ReturnsFromCache() {
        // Arrange
        Map<String, Object> expectedStats = Map.of(
                "hitRate", BigDecimal.valueOf(80.0),
                "average", BigDecimal.valueOf(22.5),
                "successCount", 8,
                "failureCount", 2,
                "category", StatCategory.POINTS,
                "threshold", 20
        );
        when(cacheService.getHitRate(
                any(Players.class),
                any(StatCategory.class),
                any(Integer.class),
                any(TimePeriod.class))).thenReturn(expectedStats);

        // Act
        Map<String, Object> result = statsService.calculateHitRate(
                testPlayer, StatCategory.POINTS, 20, TimePeriod.L10);

        // Assert
        assertThat(result)
                .as("Cache hit should return exact cached values")
                .isEqualTo(expectedStats);

        verify(gameStatsRepository, never())
                .findPlayerRecentGames(any(Players.class));
    }

    @Test
    void calculateHitRate_CacheMiss_CalculatesFromGames() {
        // Arrange
        when(cacheService.getHitRate(
                any(Players.class),
                any(StatCategory.class),
                any(Integer.class),
                any(TimePeriod.class))).thenReturn(null);
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(testGames);

        // Act
        Map<String, Object> result = statsService.calculateHitRate(
                testPlayer, StatCategory.POINTS, 20, TimePeriod.L10);

        // Assert
        assertThat(result)
                .as("Result should contain all required fields")
                .containsKeys("hitRate", "average", "successCount", "failureCount", "category", "threshold");

        // For BigDecimal comparisons, need to cast and use isEqualByComparingTo
        assertThat((BigDecimal) result.get("hitRate"))
                .as("Hit rate should be 100% as all games are 20+ points")
                .isEqualByComparingTo("100.00");

        assertThat((BigDecimal) result.get("average"))
                .as("Average should be 24.50 (mean of 20-29)")
                .isEqualByComparingTo("24.50");

        // Non-BigDecimal comparisons can use regular isEqualTo
        assertThat(result.get("successCount"))
                .as("All 10 games should be successes")
                .isEqualTo(10);

        assertThat(result.get("failureCount"))
                .as("No games should be failures")
                .isEqualTo(0);

        assertThat(result.get("category"))
                .as("Category should be POINTS")
                .isEqualTo(StatCategory.POINTS);

        assertThat(result.get("threshold"))
                .as("Threshold should be 20")
                .isEqualTo(20);

        verify(gameStatsRepository).findPlayerRecentGames(any(Players.class));
    }

    @Test
    void getPlayerAverages_ReturnsCorrectAverages() {
        // Arrange
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(testGames);

        // Act
        Map<StatCategory, BigDecimal> averages = statsService.getPlayerAverages(testPlayer, TimePeriod.L10);

        // Assert
        assertThat(averages)
                .as("Should contain averages for all stat categories")
                .containsKeys(StatCategory.POINTS, StatCategory.ASSISTS, StatCategory.REBOUNDS);

        assertThat(averages.get(StatCategory.POINTS))
                .as("Points average should be 24.50")
                .isEqualByComparingTo("24.50");

        assertThat(averages.values())
                .as("All averages should have max 2 decimal places")
                .allMatch(avg -> avg.scale() <= 2);
    }

    @Test
    void hasSufficientData_WithEnoughGames_ReturnsTrue() {
        // Arrange
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(testGames);

        // Act
        boolean result = statsService.hasSufficientData(testPlayer, TimePeriod.L10);

        // Assert
        assertThat(result)
                .as("10 games should be sufficient for L10 period")
                .isTrue();
    }

    @Test
    void hasSufficientData_WithInsufficientGames_ReturnsFalse() {
        // Arrange
        List<GameStats> insufficientGames = testGames.subList(0, 5);
        when(cacheService.getPlayerStats(any(Players.class), any(TimePeriod.class))).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(insufficientGames);

        // Act
        boolean result = statsService.hasSufficientData(testPlayer, TimePeriod.L10);

        // Assert
        assertThat(result)
                .as("5 games should not be sufficient for L10 period")
                .isFalse();
    }

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
}