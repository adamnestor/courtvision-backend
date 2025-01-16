package com.adamnestor.courtvision.test.service;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.service.cache.CacheMonitoringService;
import com.adamnestor.courtvision.service.cache.GameStatsCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameStatsCacheServiceTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private CacheMonitoringService monitoringService;

    @Mock
    private Players mockPlayer;

    private GameStatsCacheService gameStatsCacheService;
    private List<GameStats> mockGameStats;

    @BeforeEach
    void setUp() {
        gameStatsCacheService = new GameStatsCacheService(gameStatsRepository, monitoringService);
        
        // Create mock game stats
        mockGameStats = Arrays.asList(
            createGameStats(30, 5, 5),
            createGameStats(25, 8, 6),
            createGameStats(28, 4, 7),
            createGameStats(22, 6, 8),
            createGameStats(35, 7, 4)
        );
    }

    @Test
    void getPlayerStats_L5Period_ReturnsLimitedStats() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(mockPlayer)).thenReturn(mockGameStats);

        // Act
        List<GameStats> result = gameStatsCacheService.getPlayerStats(mockPlayer, TimePeriod.L5);

        // Assert
        assertEquals(5, result.size());
        verify(monitoringService).recordCacheAccess(false);
        verify(gameStatsRepository).findPlayerRecentGames(mockPlayer);
    }

    @Test
    void getPlayerStats_L10Period_ReturnsAvailableStats() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(mockPlayer)).thenReturn(mockGameStats);

        // Act
        List<GameStats> result = gameStatsCacheService.getPlayerStats(mockPlayer, TimePeriod.L10);

        // Assert
        assertEquals(5, result.size()); // Should return all available stats since we only have 5
        verify(monitoringService).recordCacheAccess(false);
    }

    @Test
    void getPlayerStats_HandlesException() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(mockPlayer))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            gameStatsCacheService.getPlayerStats(mockPlayer, TimePeriod.L5)
        );
        verify(monitoringService).recordError();
    }

    @Test
    void getPlayerAverages_CalculatesCorrectAverages() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(mockPlayer)).thenReturn(mockGameStats);

        // Act
        Map<String, Double> averages = gameStatsCacheService.getPlayerAverages(mockPlayer, TimePeriod.L5);

        // Assert
        assertEquals(28.0, averages.get("points"), 0.01);
        assertEquals(6.0, averages.get("assists"), 0.01);
        assertEquals(6.0, averages.get("rebounds"), 0.01);
        verify(monitoringService).recordCacheAccess(false);
    }

    @Test
    void getPlayerAverages_EmptyStats_ReturnsZeros() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(mockPlayer)).thenReturn(List.of());

        // Act
        Map<String, Double> averages = gameStatsCacheService.getPlayerAverages(mockPlayer, TimePeriod.L5);

        // Assert
        assertEquals(0.0, averages.get("points"));
        assertEquals(0.0, averages.get("assists"));
        assertEquals(0.0, averages.get("rebounds"));
        verify(monitoringService).recordCacheAccess(false);
    }

    @Test
    void getPlayerAverages_HandlesException() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(mockPlayer))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            gameStatsCacheService.getPlayerAverages(mockPlayer, TimePeriod.L5)
        );
        verify(monitoringService).recordError();
    }

    private GameStats createGameStats(int points, int assists, int rebounds) {
        GameStats stats = new GameStats();
        stats.setPoints(points);
        stats.setAssists(assists);
        stats.setRebounds(rebounds);
        return stats;
    }
} 