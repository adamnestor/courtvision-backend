package com.adamnestor.courtvision.test.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.service.cache.CacheMonitoringService;
import com.adamnestor.courtvision.service.cache.HitRateCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HitRateCacheServiceTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private CacheMonitoringService monitoringService;

    private HitRateCacheService hitRateCacheService;
    private Players testPlayer;
    private List<GameStats> mockGameStats;

    @BeforeEach
    void setUp() {
        hitRateCacheService = new HitRateCacheService(gameStatsRepository, monitoringService);

        // Setup test player
        testPlayer = new Players();
        testPlayer.setId(1L);
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");

        // Setup mock game stats
        mockGameStats = createMockGameStats();
    }

    @Test
    void getHitRate_ShouldCalculateCorrectly_ForPoints() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(mockGameStats);

        // Act
        Map<String, Object> result = hitRateCacheService.getHitRate(
                testPlayer, StatCategory.POINTS, 20, TimePeriod.L5);

        // Assert
        assertNotNull(result);
        assertEquals(60.0, result.get("hitRate")); // 3 out of 5 games above 20 points
        assertEquals(21.0, result.get("average")); // Average of points
        assertEquals(5, result.get("gamesPlayed"));
        assertEquals(TimePeriod.L5, result.get("period"));
        assertEquals(20, result.get("threshold"));
        assertEquals(StatCategory.POINTS, result.get("category"));
        assertNotNull(result.get("calculatedAt"));
        assertTrue(result.get("calculatedAt") instanceof java.time.LocalDateTime);
        verify(monitoringService).recordCacheAccess(false);
    }

    @Test
    void getHitRate_ShouldCalculateCorrectly_ForAssists() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(mockGameStats);

        // Act
        Map<String, Object> result = hitRateCacheService.getHitRate(
                testPlayer, StatCategory.ASSISTS, 5, TimePeriod.L5);

        // Assert
        assertNotNull(result);
        assertEquals(40.0, result.get("hitRate")); // 2 out of 5 games above 5 assists
        assertEquals(4.4, result.get("average")); // Average of assists
        verify(monitoringService).recordCacheAccess(false);
    }

    @Test
    void getHitRate_ShouldHandleEmptyGameList() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(List.of());

        // Act
        Map<String, Object> result = hitRateCacheService.getHitRate(
                testPlayer, StatCategory.POINTS, 20, TimePeriod.L5);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.get("hitRate"));
        assertEquals(0.0, result.get("average"));
        assertEquals(0, result.get("gamesPlayed"));
        verify(monitoringService).recordCacheAccess(false);
    }

    @Test
    void getHitRate_ShouldHandleRepositoryException() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                hitRateCacheService.getHitRate(testPlayer, StatCategory.POINTS, 20, TimePeriod.L5)
        );
        verify(monitoringService).recordError();
    }

    @Test
    void getHitRate_ShouldCalculateCorrectly_ForRebounds() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(mockGameStats);

        // Act
        Map<String, Object> result = hitRateCacheService.getHitRate(
                testPlayer, StatCategory.REBOUNDS, 6, TimePeriod.L5);

        // Assert
        assertNotNull(result);
        assertEquals(60.0, result.get("hitRate")); // 3 out of 5 games >= 6 rebounds
        assertEquals(6.0, result.get("average")); // Average of rebounds
        assertEquals(5, result.get("gamesPlayed"));
        assertEquals(TimePeriod.L5, result.get("period"));
        assertEquals(6, result.get("threshold"));
        assertEquals(StatCategory.REBOUNDS, result.get("category"));
        verify(monitoringService).recordCacheAccess(false);
    }

    @Test
    void getHitRate_ShouldLimitGamesBasedOnTimePeriod() {
        // Arrange
        List<GameStats> moreGames = Arrays.asList(
                createGameStats(25, 6, 8),
                createGameStats(18, 4, 5),
                createGameStats(22, 5, 7),
                createGameStats(15, 3, 4),
                createGameStats(25, 4, 6),
                createGameStats(30, 7, 9),
                createGameStats(28, 6, 8),
                createGameStats(19, 5, 7),
                createGameStats(21, 4, 6),
                createGameStats(24, 5, 7)
        );
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(moreGames);

        // Act
        Map<String, Object> result = hitRateCacheService.getHitRate(
                testPlayer, StatCategory.POINTS, 20, TimePeriod.L5);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.get("gamesPlayed")); // Should only use first 5 games
        assertEquals(TimePeriod.L5, result.get("period"));
        verify(monitoringService).recordCacheAccess(false);
    }

    @Test
    void getHitRate_ShouldThrowException_ForInvalidCategory() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                hitRateCacheService.getHitRate(testPlayer, null, 20, TimePeriod.L5)
        );
        assertEquals("Category cannot be null", exception.getMessage());
        verify(monitoringService).recordError();
    }

    private List<GameStats> createMockGameStats() {
        return Arrays.asList(
                createGameStats(25, 6, 8),  // Above threshold
                createGameStats(18, 4, 5),  // Below threshold
                createGameStats(22, 5, 7),  // Above threshold
                createGameStats(15, 3, 4),  // Below threshold
                createGameStats(25, 4, 6)   // Above threshold
        );
    }

    private GameStats createGameStats(int points, int assists, int rebounds) {
        GameStats stats = new GameStats();
        stats.setPoints(points);
        stats.setAssists(assists);
        stats.setRebounds(rebounds);
        return stats;
    }
}