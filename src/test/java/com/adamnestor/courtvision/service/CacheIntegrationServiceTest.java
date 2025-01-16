package com.adamnestor.courtvision.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adamnestor.courtvision.service.cache.CacheMonitoringService;

@ExtendWith(MockitoExtension.class)
public class CacheIntegrationServiceTest {

    @Mock
    private DailyRefreshService dailyRefreshService;

    @Mock
    private WarmingStrategyService warmingStrategyService;

    @Mock
    private CacheMonitoringService monitoringService;

    @InjectMocks
    private CacheIntegrationService cacheIntegrationService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(dailyRefreshService, warmingStrategyService, monitoringService);
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
        assertTrue(monitoringService.checkHealth() == false, "Health check should return false");
    }

    @Test
    void performDailyUpdate_DailyRefreshThrowsException() {
        // Arrange
        when(monitoringService.checkHealth()).thenReturn(true);
        doThrow(new RuntimeException("Refresh failed")).when(dailyRefreshService).performDailyRefresh();

        // Act
        cacheIntegrationService.performDailyUpdate();

        // Assert
        verify(monitoringService).checkHealth();
        verify(dailyRefreshService).performDailyRefresh();
        verify(warmingStrategyService, never()).executeWarmingStrategy(any());
    }

    @Test
    void performDailyUpdate_WarmingStrategyThrowsException() {
        // Arrange
        when(monitoringService.checkHealth()).thenReturn(true);
        doThrow(new RuntimeException("Warming failed"))
            .when(warmingStrategyService)
            .executeWarmingStrategy(any());

        // Act
        cacheIntegrationService.performDailyUpdate();

        // Assert
        verify(monitoringService).checkHealth();
        verify(dailyRefreshService).performDailyRefresh();
        verify(warmingStrategyService).executeWarmingStrategy(WarmingStrategyService.WarmingPriority.HIGH);
    }
} 