package com.adamnestor.courtvision.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.adamnestor.courtvision.service.cache.CacheWarmingService;
import com.adamnestor.courtvision.service.cache.HitRateCacheService;

@ExtendWith(MockitoExtension.class)
class DailyRefreshServiceTest {

    @Mock
    private CacheWarmingService cacheWarmingService;

    @Mock
    private HitRateCacheService hitRateCacheService;

    private DailyRefreshService dailyRefreshService;

    @BeforeEach
    void setUp() {
        dailyRefreshService = new DailyRefreshService();
        ReflectionTestUtils.setField(dailyRefreshService, "cacheWarmingService", cacheWarmingService);
        ReflectionTestUtils.setField(dailyRefreshService, "hitRateCacheService", hitRateCacheService);
    }

    @Test
    void performDailyRefresh_SuccessfulExecution() {
        // When
        dailyRefreshService.performDailyRefresh();

        // Then
        verify(cacheWarmingService).warmTodaysGames();
        verify(dailyRefreshService).refreshPlayerStats();
        verify(dailyRefreshService).refreshHitRates();
    }

    @Test
    void performDailyRefresh_HandlesWarmingError() {
        // Given
        doThrow(new RuntimeException("Warming error"))
            .when(cacheWarmingService)
            .warmTodaysGames();

        // When
        dailyRefreshService.performDailyRefresh();

        // Then
        verify(dailyRefreshService).initiateErrorRecovery();
    }

    @Test
    void refreshPlayerStats_SuccessfulExecution() {
        // When
        dailyRefreshService.refreshPlayerStats();

        // Then
        // Verify internal operations once implemented
        verifyNoMoreInteractions(cacheWarmingService);
    }

    @Test
    void refreshHitRates_SuccessfulExecution() {
        // When
        dailyRefreshService.refreshHitRates();

        // Then
        // Verify internal operations once implemented
        verifyNoMoreInteractions(hitRateCacheService);
    }

    @Test
    void initiateErrorRecovery_ExecutesRecoverySteps() {
        // When
        dailyRefreshService.initiateErrorRecovery();

        // Then
        // Verify recovery steps once implemented
        verifyNoMoreInteractions(cacheWarmingService, hitRateCacheService);
    }

    @Test
    void refreshTodaysGames_CallsCacheWarming() {
        // When
        dailyRefreshService.refreshTodaysGames();

        // Then
        verify(cacheWarmingService).warmTodaysGames();
    }

    @Test
    void performDailyRefresh_HandlesMultipleErrors() {
        // Given
        doThrow(new RuntimeException("First error"))
            .when(cacheWarmingService)
            .warmTodaysGames();

        // When
        dailyRefreshService.performDailyRefresh();
        dailyRefreshService.performDailyRefresh();

        // Then
        verify(dailyRefreshService, times(2)).initiateErrorRecovery();
    }

    @Test
    void performDailyRefresh_CompletesAllSteps() {
        // Given
        when(cacheWarmingService.warmTodaysGames()).thenReturn(true);

        // When
        dailyRefreshService.performDailyRefresh();

        // Then
        verify(cacheWarmingService).warmTodaysGames();
        verify(dailyRefreshService).refreshPlayerStats();
        verify(dailyRefreshService).refreshHitRates();
        verifyNoMoreInteractions(cacheWarmingService);
    }
} 