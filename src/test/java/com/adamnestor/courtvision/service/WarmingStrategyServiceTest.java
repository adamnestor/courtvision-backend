package com.adamnestor.courtvision.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adamnestor.courtvision.service.cache.CacheMonitoringService;
import com.adamnestor.courtvision.service.WarmingStrategyService.WarmingPriority;

@ExtendWith(MockitoExtension.class)
class WarmingStrategyServiceTest {

    @Mock
    private CacheMonitoringService monitoringService;

    @InjectMocks
    private WarmingStrategyService warmingStrategyService;

    @BeforeEach
    void setUp() {
        // Additional setup if needed
    }

    @Test
    void executeWarmingStrategy_HighPriority_ShouldWarmRegularData() {
        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.HIGH);

        // Then
        verify(monitoringService, never()).recordError();
    }

    @Test
    void executeWarmingStrategy_MediumPriority_ShouldWarmOptionalData() {
        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.MEDIUM);

        // Then
        verify(monitoringService, never()).recordError();
    }

    @Test
    void executeWarmingStrategy_LowPriority_ShouldSkipWarming() {
        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.LOW);

        // Then
        verify(monitoringService, never()).recordError();
    }

    @Test
    void warmRegularData_Success() {
        // When
        warmingStrategyService.warmRegularData();

        // Then
        verify(monitoringService, never()).recordError();
    }

    @Test
    void warmOptionalData_Success() {
        // When
        warmingStrategyService.warmOptionalData();

        // Then
        verify(monitoringService, never()).recordError();
    }

    @Test
    void executeWarmingStrategy_HandlesError() {
        // Given
        doThrow(new RuntimeException("Warming error"))
            .when(monitoringService)
            .recordCacheAccess(anyBoolean());

        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.HIGH);

        // Then
        verify(monitoringService, times(2)).recordError();
    }

    @Test
    void executeWarmingStrategy_NullPriority_ShouldHandleGracefully() {
        // When
        warmingStrategyService.executeWarmingStrategy(null);

        // Then
        verify(monitoringService, never()).recordError();
    }

    @Test
    void warmRegularData_HandlesError() {
        // Given
        doThrow(new RuntimeException("Warming error"))
            .when(monitoringService)
            .recordCacheAccess(anyBoolean());

        // When
        warmingStrategyService.warmRegularData();

        // Then
        verify(monitoringService, times(1)).recordError();
    }
} 