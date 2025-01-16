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