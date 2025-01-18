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
        verify(dailyRefreshService).updatePlayerStats();
        verify(dailyRefreshService).updateHitRateCalculations();
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
        // Error is logged but no recovery yet
        verifyNoMoreInteractions(dailyRefreshService);
    }

    @Test
    void updatePlayerStats_SuccessfulExecution() {
        // When
        dailyRefreshService.updatePlayerStats();

        // Then
        verifyNoMoreInteractions(cacheWarmingService);
    }

    @Test
    void updateHitRateCalculations_SuccessfulExecution() {
        // When
        dailyRefreshService.updateHitRateCalculations();

        // Then
        verifyNoMoreInteractions(hitRateCacheService);
    }

    @Test
    void refreshTodaysGames_CallsCacheWarming() {
        // When
        dailyRefreshService.refreshTodaysGames();

        // Then
        verify(cacheWarmingService).warmTodaysGames();
    }

    @Test
    void performDailyRefresh_CompletesAllSteps() {
        // Given
        when(cacheWarmingService.warmTodaysGames()).thenReturn(true);

        // When
        dailyRefreshService.performDailyRefresh();

        // Then
        verify(cacheWarmingService).warmTodaysGames();
        verify(dailyRefreshService).updatePlayerStats();
        verify(dailyRefreshService).updateHitRateCalculations();
        verifyNoMoreInteractions(cacheWarmingService);
    }
} 