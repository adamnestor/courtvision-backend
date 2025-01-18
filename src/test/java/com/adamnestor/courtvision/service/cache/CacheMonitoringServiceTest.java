package com.adamnestor.courtvision.service.cache;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheMonitoringServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private MeterRegistry meterRegistry;
    private CacheMonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        monitoringService = new CacheMonitoringServiceImpl(meterRegistry, redisTemplate);
    }

    @Test
    void recordCacheAccess_Hit_ShouldIncrementHitCounter() {
        // When
        monitoringService.recordCacheAccess(true);

        // Then
        assertEquals(1.0, meterRegistry.counter("cache.hits").count());
        assertEquals(0.0, meterRegistry.counter("cache.misses").count());
    }

    @Test
    void recordCacheAccess_Miss_ShouldIncrementMissCounter() {
        // When
        monitoringService.recordCacheAccess(false);

        // Then
        assertEquals(0.0, meterRegistry.counter("cache.hits").count());
        assertEquals(1.0, meterRegistry.counter("cache.misses").count());
    }

    @Test
    void recordError_ShouldIncrementErrorCounter() {
        // When
        monitoringService.recordError(new RuntimeException("Test error"));

        // Then
        assertEquals(1.0, meterRegistry.counter("cache.errors").count());
    }

    @Test
    @SuppressWarnings("unchecked")
    void performHealthCheck_WhenRedisIsHealthy_ShouldReturnTrue() {
        // Given
        RedisConnection mockConnection = mock(RedisConnection.class);
        RedisServerCommands mockServerCommands = mock(RedisServerCommands.class);
        Properties mockMemoryInfo = new Properties();
        mockMemoryInfo.setProperty("used_memory", "1024");
        
        when(mockConnection.serverCommands()).thenReturn(mockServerCommands);
        when(mockServerCommands.info("memory")).thenReturn(mockMemoryInfo);
        when(mockServerCommands.dbSize()).thenReturn(100L);
        when(redisTemplate.execute(any(RedisCallback.class), anyBoolean())).thenAnswer(invocation -> {
            RedisCallback<Object> callback = (RedisCallback<Object>) invocation.getArgument(0);
            return callback.doInRedis(mockConnection);
        });

        // When
        boolean result = monitoringService.performHealthCheck();

        // Then
        assertTrue(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void performHealthCheck_WhenRedisIsUnhealthy_ShouldReturnFalse() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class), anyBoolean())).thenThrow(new RuntimeException("Redis error"));

        // When
        boolean result = monitoringService.performHealthCheck();

        // Then
        assertFalse(result);
        assertEquals(1.0, meterRegistry.counter("cache.errors").count());
    }

    @Test
    void calculateHitRate_ShouldReturnCorrectPercentage() {
        // Given
        monitoringService.recordCacheAccess(true);  // hit
        monitoringService.recordCacheAccess(true);  // hit
        monitoringService.recordCacheAccess(false); // miss

        // When
        double hitRate = monitoringService.getHitRate();

        // Then
        assertEquals(66.67, hitRate, 0.01);
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectPerformanceMetrics_ShouldRecordAllMetrics() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class), anyBoolean())).thenReturn(true);

        // When
        monitoringService.performHealthCheck();

        // Then
        assertNotNull(meterRegistry.find("cache.memory.usage").gauge());
        assertNotNull(meterRegistry.find("cache.keys.total").gauge());
        assertNotNull(meterRegistry.find("cache.hit.rate").gauge());
    }

    @Test
    void getHitRate_ShouldReturnOneWhenNoAccesses() {
        // When
        double hitRate = monitoringService.getHitRate();

        // Then
        assertEquals(1.0, hitRate);
    }

    @Test
    void getErrorRate_ShouldCalculateCorrectly() {
        // Given
        monitoringService.recordCacheAccess(true);   // operation
        monitoringService.recordCacheAccess(false);  // operation
        monitoringService.recordError(new RuntimeException("Test error")); // error

        // When
        double errorRate = monitoringService.getErrorRate();

        // Then
        assertEquals(0.5, errorRate, 0.01);
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkHealth_ShouldCallPerformHealthCheck() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class), anyBoolean())).thenReturn(true);

        // When
        boolean result = monitoringService.checkHealth();

        // Then
        assertTrue(result);
    }
} 