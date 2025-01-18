package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.cache.CacheMonitoringService;
import com.adamnestor.courtvision.service.cache.CacheKeyGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CacheIntegrationServiceTest {

    @Mock
    private DailyRefreshService dailyRefreshService;

    @Mock
    private WarmingStrategyService warmingStrategyService;

    @Mock
    private CacheMonitoringService monitoringService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private CacheKeyGenerator keyGenerator;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private PlayersRepository playersRepository;

    @InjectMocks
    private CacheIntegrationService cacheIntegrationService;

    @Test
    void performDailyUpdate_Success() {
        // Arrange
        when(monitoringService.performHealthCheck()).thenReturn(true);

        // Act
        cacheIntegrationService.performDailyUpdate();

        // Assert
        verify(monitoringService).performHealthCheck();
        verify(dailyRefreshService).performDailyRefresh();
        verify(warmingStrategyService).implementPriorityWarming();
    }

    @Test
    void performDailyUpdate_HealthCheckFails() {
        // Arrange
        when(monitoringService.performHealthCheck()).thenReturn(false);

        // Act
        cacheIntegrationService.performDailyUpdate();

        // Assert
        verify(monitoringService).performHealthCheck();
        verify(dailyRefreshService, never()).performDailyRefresh();
        verify(warmingStrategyService, never()).implementPriorityWarming();
    }

    @Test
    void verifyDataSynchronization_Success() {
        // Arrange
        Set<String> mockKeys = new HashSet<>(Arrays.asList("player:stats:1", "player:hitrate:1"));
        when(redisTemplate.keys("*")).thenReturn(mockKeys);
        when(redisTemplate.keys("player:stats:*")).thenReturn(new HashSet<>(Arrays.asList("player:stats:1")));
        when(redisTemplate.keys("player:hitrate:*")).thenReturn(new HashSet<>(Arrays.asList("player:hitrate:1")));
        when(redisTemplate.getExpire(anyString())).thenReturn(3600L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Mock hit rate data
        Map<String, Object> hitRateData = new HashMap<>();
        hitRateData.put("hitRate", 85.0);
        hitRateData.put("average", 75.0);
        hitRateData.put("gamesPlayed", 10);
        when(valueOperations.get("player:hitrate:1")).thenReturn(hitRateData);
        
        // Mock player stats data
        GameStats mockStats = createMockGameStats();
        when(valueOperations.get("player:stats:1")).thenReturn(Collections.singletonList(mockStats));
        
        // Mock monitoring service metrics
        when(monitoringService.getHitRate()).thenReturn(0.85);
        when(monitoringService.getErrorRate()).thenReturn(0.01);
        
        // Mock player data consistency
        Players mockPlayer = new Players();
        mockPlayer.setId(1L);
        when(playersRepository.findById(1L)).thenReturn(Optional.of(mockPlayer));
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class)))
            .thenReturn(Collections.singletonList(mockStats));

        // Act
        boolean result = cacheIntegrationService.verifyDataSynchronization();

        // Assert
        assertTrue(result);
        verify(redisTemplate, atLeastOnce()).keys(anyString());
    }

    @Test
    void verifyDataSynchronization_FailsOnInvalidKeys() {
        // Arrange
        Set<String> mockKeys = new HashSet<>(Arrays.asList("invalid:key:1"));
        when(redisTemplate.keys(anyString())).thenReturn(mockKeys);

        // Act
        boolean result = cacheIntegrationService.verifyDataSynchronization();

        // Assert
        assertFalse(result);
    }

    @Test
    void handleUpdateFailure_ShouldRetryAndReport() {
        // Arrange
        String updateType = "daily-update";
        when(monitoringService.performHealthCheck()).thenReturn(true);
        when(monitoringService.getErrorRate()).thenReturn(0.05);
        when(monitoringService.getHitRate()).thenReturn(0.75);
        when(redisTemplate.keys("*")).thenReturn(new HashSet<>(Arrays.asList("player:stats:1")));
        
        doThrow(new RuntimeException("First try fails"))
            .doNothing()
            .when(dailyRefreshService)
            .performDailyRefresh();

        // Act
        cacheIntegrationService.handleUpdateFailure(updateType);

        // Assert
        verify(dailyRefreshService, times(2)).performDailyRefresh();
        verify(monitoringService, atLeastOnce()).performHealthCheck();
        verify(redisTemplate).opsForValue();
    }

    @Test
    void handleUpdateFailure_MaxRetriesExceeded() {
        // Arrange
        String updateType = "daily-update";
        when(monitoringService.performHealthCheck()).thenReturn(true);
        when(monitoringService.getErrorRate()).thenReturn(0.05);
        when(monitoringService.getHitRate()).thenReturn(0.75);
        when(redisTemplate.keys("*")).thenReturn(new HashSet<>(Arrays.asList("player:stats:1")));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        doThrow(new RuntimeException("Always fails"))
            .when(dailyRefreshService)
            .performDailyRefresh();

        // Act
        cacheIntegrationService.handleUpdateFailure(updateType);

        // Assert
        verify(dailyRefreshService, times(3)).performDailyRefresh();
        verify(monitoringService, atLeastOnce()).performHealthCheck();
        verify(redisTemplate, times(2)).opsForValue();
    }

    @Test
    void checkDataConsistency_Success() {
        // Arrange
        Set<String> playerKeys = new HashSet<>(Arrays.asList("player:stats:1"));
        Set<String> hitRateKeys = new HashSet<>(Arrays.asList("player:hitrate:1"));
        
        // Mock keys queries
        when(redisTemplate.keys("player:stats:*")).thenReturn(playerKeys);
        when(redisTemplate.keys("player:hitrate:*")).thenReturn(hitRateKeys);
        when(redisTemplate.keys("*")).thenReturn(new HashSet<>(Arrays.asList("player:stats:1", "player:hitrate:1")));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Mock TTL check
        when(redisTemplate.getExpire(anyString())).thenReturn(3600L);
        
        // Mock player lookup
        Players mockPlayer = new Players();
        mockPlayer.setId(1L);
        when(playersRepository.findById(1L)).thenReturn(Optional.of(mockPlayer));
        
        // Mock the cached data
        GameStats mockStats = createMockGameStats();
        when(valueOperations.get("player:stats:1")).thenReturn(Collections.singletonList(mockStats));
        
        // Mock hit rate data
        Map<String, Object> hitRateData = new HashMap<>();
        hitRateData.put("hitRate", 85.0);
        hitRateData.put("average", 75.0);
        hitRateData.put("gamesPlayed", 10);
        when(valueOperations.get("player:hitrate:1")).thenReturn(hitRateData);
        
        // Mock database data
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class)))
            .thenReturn(Collections.singletonList(mockStats));
        
        // Mock monitoring service metrics
        when(monitoringService.getHitRate()).thenReturn(0.85);
        when(monitoringService.getErrorRate()).thenReturn(0.01);

        // Act
        boolean result = cacheIntegrationService.verifyDataSynchronization();

        // Assert
        assertTrue(result);
        verify(redisTemplate, atLeastOnce()).keys(anyString());
        verify(gameStatsRepository, atLeastOnce()).findPlayerRecentGames(any(Players.class));
        verify(redisTemplate, atLeastOnce()).getExpire(anyString());
    }

    private GameStats createMockGameStats() {
        GameStats stats = new GameStats();
        stats.setPoints(20);
        stats.setAssists(5);
        stats.setRebounds(8);
        return stats;
    }
} 