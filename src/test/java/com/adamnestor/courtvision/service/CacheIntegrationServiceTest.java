package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.service.cache.CacheMonitoringService;
import com.adamnestor.courtvision.service.cache.CacheKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
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

    @InjectMocks
    private CacheIntegrationService cacheIntegrationService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void performDailyUpdate_Success() {
        // Arrange
        when(monitoringService.checkHealth()).thenReturn(true);

        // Act
        cacheIntegrationService.performDailyUpdate();

        // Assert
        verify(monitoringService).checkHealth();
        verify(dailyRefreshService).performDailyRefresh();
        verify(warmingStrategyService).executeWarmingStrategy(WarmingStrategyService.WarmingPriority.HIGH);
    }

    @Test
    void performDailyUpdate_HealthCheckFails() {
        // Arrange
        when(monitoringService.checkHealth()).thenReturn(false);

        // Act
        cacheIntegrationService.performDailyUpdate();

        // Assert
        verify(monitoringService).checkHealth();
        verify(dailyRefreshService, never()).performDailyRefresh();
        verify(warmingStrategyService, never()).executeWarmingStrategy(any());
    }

    @Test
    void verifyDataSynchronization_Success() {
        // Arrange
        Set<String> mockKeys = new HashSet<>(Arrays.asList("player:stats:1", "player:hitrate:1"));
        when(redisTemplate.keys(anyString())).thenReturn(mockKeys);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        when(redisTemplate.getExpire(anyString())).thenReturn(3600L);

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
        when(monitoringService.checkHealth()).thenReturn(true);
        doThrow(new RuntimeException("First try fails"))
            .doNothing()
            .when(dailyRefreshService)
            .performDailyRefresh();

        // Act
        cacheIntegrationService.handleUpdateFailure(updateType);

        // Assert
        verify(dailyRefreshService, times(2)).performDailyRefresh();
        verify(redisTemplate).opsForValue();
    }

    @Test
    void handleUpdateFailure_MaxRetriesExceeded() {
        // Arrange
        String updateType = "daily-update";
        when(monitoringService.checkHealth()).thenReturn(true);
        doThrow(new RuntimeException("Always fails"))
            .when(dailyRefreshService)
            .performDailyRefresh();

        // Act
        cacheIntegrationService.handleUpdateFailure(updateType);

        // Assert
        verify(dailyRefreshService, times(3)).performDailyRefresh(); // Max retries
        verify(redisTemplate).opsForValue();
    }

    @Test
    void checkDataConsistency_Success() {
        // Arrange
        Set<String> playerKeys = new HashSet<>(Arrays.asList("player:stats:1"));
        Set<String> hitRateKeys = new HashSet<>(Arrays.asList("player:hitrate:1"));
        when(redisTemplate.keys("player:stats:*")).thenReturn(playerKeys);
        when(redisTemplate.keys("player:hitrate:*")).thenReturn(hitRateKeys);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        when(redisTemplate.getExpire(anyString())).thenReturn(3600L);

        // Mock player stats data
        List<GameStats> mockStats = Collections.singletonList(createMockGameStats());
        when(valueOperations.get(anyString())).thenReturn(mockStats);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class))).thenReturn(mockStats);

        // Act
        boolean result = cacheIntegrationService.verifyDataSynchronization();

        // Assert
        assertTrue(result);
        verify(redisTemplate, atLeastOnce()).keys(anyString());
        verify(gameStatsRepository, atLeastOnce()).findPlayerRecentGames(any(Players.class));
    }

    private GameStats createMockGameStats() {
        GameStats stats = new GameStats();
        stats.setPoints(20);
        stats.setAssists(5);
        stats.setRebounds(8);
        return stats;
    }
} 