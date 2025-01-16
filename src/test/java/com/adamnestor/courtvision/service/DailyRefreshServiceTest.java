package com.adamnestor.courtvision.service;

import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adamnestor.courtvision.service.cache.CacheWarmingService;

@ExtendWith(MockitoExtension.class)
public class DailyRefreshServiceTest {

    @Mock
    private CacheWarmingService cacheWarmingService;

    @Spy
    private DailyRefreshService dailyRefreshService;

    @BeforeEach
    void setUp() throws Exception {
        dailyRefreshService = spy(new DailyRefreshService());
        
        // Use reflection to set the private field
        Field cacheWarmingServiceField = DailyRefreshService.class.getDeclaredField("cacheWarmingService");
        cacheWarmingServiceField.setAccessible(true);
        cacheWarmingServiceField.set(dailyRefreshService, cacheWarmingService);
    }

    @Test
    void performDailyRefresh_SuccessfulExecution_ExecutesAllRefreshes() {
        // Act
        dailyRefreshService.performDailyRefresh();

        // Assert
        verify(cacheWarmingService, times(1)).warmTodaysGames();
        verify(dailyRefreshService, times(1)).refreshPlayerStats();
        verify(dailyRefreshService, times(1)).refreshHitRates();
    }

    @Test
    void performDailyRefresh_WhenExceptionOccurs_InitiatesErrorRecovery() {
        // Arrange
        doThrow(new RuntimeException("Test error")).when(cacheWarmingService).warmTodaysGames();

        // Act
        dailyRefreshService.performDailyRefresh();

        // Assert
        verify(dailyRefreshService, times(1)).initiateErrorRecovery();
    }

    @Test
    void refreshTodaysGames_CallsCacheWarmingService() {
        // Act
        dailyRefreshService.refreshTodaysGames();

        // Assert
        verify(cacheWarmingService, times(1)).warmTodaysGames();
    }

    @Test
    void refreshPlayerStats_ExecutesSuccessfully() {
        // Act
        dailyRefreshService.refreshPlayerStats();

        // Method is currently empty, but test is ready for implementation
    }

    @Test
    void refreshHitRates_ExecutesSuccessfully() {
        // Act
        dailyRefreshService.refreshHitRates();

        // Method is currently empty, but test is ready for implementation
    }

    @Test
    void initiateErrorRecovery_ExecutesSuccessfully() {
        // Act
        dailyRefreshService.initiateErrorRecovery();

        // Method is currently empty, but test is ready for implementation
    }
} 