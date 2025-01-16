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
public class WarmingStrategyServiceTest {

    @Mock
    private CacheMonitoringService monitoringService;

    @InjectMocks
    private WarmingStrategyService warmingStrategyService;

    @BeforeEach
    void setUp() {
        // Additional setup if needed
    }

    @Test
    void executeWarmingStrategy_HighPriority_ShouldWarmCriticalData() {
        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.HIGH);

        // Then
        verify(monitoringService, times(1)).recordCacheAccess(true);
    }

    @Test
    void executeWarmingStrategy_MediumPriority_ShouldWarmRegularData() {
        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.MEDIUM);

        // Then
        verify(monitoringService, never()).recordCacheAccess(anyBoolean());
    }

    @Test
    void executeWarmingStrategy_LowPriority_ShouldWarmOptionalData() {
        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.LOW);

        // Then
        verify(monitoringService, never()).recordCacheAccess(anyBoolean());
    }

    @Test
    void executeWarmingStrategy_NullPriority_ShouldHandleGracefully() {
        // When
        warmingStrategyService.executeWarmingStrategy(null);

        // Then
        verify(monitoringService, never()).recordCacheAccess(anyBoolean());
    }
} 