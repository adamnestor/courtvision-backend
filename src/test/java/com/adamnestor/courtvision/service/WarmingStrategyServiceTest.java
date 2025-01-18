package com.adamnestor.courtvision.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.adamnestor.courtvision.service.cache.CacheMonitoringService;
import com.adamnestor.courtvision.service.WarmingStrategyService.WarmingPriority;
import com.adamnestor.courtvision.service.cache.CacheWarmingService;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WarmingStrategyServiceTest {

    @Mock
    private CacheMonitoringService monitoringService;

    @Mock
    private CacheWarmingService cacheWarmingService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private WarmingStrategyService warmingStrategyService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void executeWarmingStrategy_HighPriority_ShouldImplementPriorityWarming() {
        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.HIGH);

        // Then
        verify(cacheWarmingService).warmTodaysGames();
        verify(monitoringService, never()).recordError(any(Exception.class));
        verify(valueOperations, times(2)).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void executeWarmingStrategy_MediumPriority_ShouldImplementOptionalWarming() {
        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.MEDIUM);

        // Then
        verify(cacheWarmingService, times(7)).warmHistoricalGames(any(LocalDate.class));
        verify(monitoringService, never()).recordError(any(Exception.class));
    }

    @Test
    void executeWarmingStrategy_LowPriority_ShouldSkipWarming() {
        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.LOW);

        // Then
        verify(monitoringService, never()).recordError(any(Exception.class));
    }

    @Test
    void executeWarmingStrategy_ShouldHandleErrors() {
        // Given
        RuntimeException testException = new RuntimeException("Warming error");
        doThrow(testException)
            .when(cacheWarmingService)
            .warmTodaysGames();

        // When
        warmingStrategyService.executeWarmingStrategy(WarmingPriority.HIGH);

        // Then
        verify(monitoringService).recordError(eq(testException));
    }

    @Test
    void executeWarmingStrategy_NullPriority_ShouldHandleGracefully() {
        // When
        warmingStrategyService.executeWarmingStrategy(null);

        // Then
        verify(monitoringService, never()).recordError(any(Exception.class));
    }
} 