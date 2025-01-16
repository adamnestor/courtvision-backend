package com.adamnestor.courtvision.test.cache;

import com.adamnestor.courtvision.service.cache.CacheMonitoringService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisCallback;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheMonitoringServiceTest {

    private CacheMonitoringService monitoringService;
    private MeterRegistry meterRegistry;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        monitoringService = new CacheMonitoringService(meterRegistry, redisTemplate);
    }

    @Test
    void recordCacheAccess_ShouldIncrementHitCounter() {
        // When
        monitoringService.recordCacheAccess(true);

        // Then
        assertEquals(1.0, meterRegistry.counter("cache.hits").count());
        assertEquals(0.0, meterRegistry.counter("cache.misses").count());
    }

    @Test
    void recordCacheAccess_ShouldIncrementMissCounter() {
        // When
        monitoringService.recordCacheAccess(false);

        // Then
        assertEquals(0.0, meterRegistry.counter("cache.hits").count());
        assertEquals(1.0, meterRegistry.counter("cache.misses").count());
    }

    @Test
    void getHitRate_ShouldCalculateCorrectly() {
        // Given
        monitoringService.recordCacheAccess(true);   // hit
        monitoringService.recordCacheAccess(true);   // hit
        monitoringService.recordCacheAccess(false);  // miss

        // When
        double hitRate = monitoringService.getHitRate();

        // Then
        assertEquals(0.67, hitRate, 0.01);
    }

    @Test
    void getHitRate_ShouldReturnOneWhenNoAccesses() {
        // When
        double hitRate = monitoringService.getHitRate();

        // Then
        assertEquals(1.0, hitRate);
    }

    @Test
    void recordError_ShouldIncrementErrorCounter() {
        // When
        monitoringService.recordError();

        // Then
        assertEquals(1.0, meterRegistry.counter("cache.errors").count());
    }

    @Test
    void monitorCache_ShouldHandleRedisError() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("Redis error"));

        // When
        monitoringService.monitorCache();

        // Then
        assertEquals(1.0, meterRegistry.counter("cache.errors").count());
    }

    @Test
    void checkCacheSize_ShouldHandleSuccessfulMemoryCheck() {
        // Given
        RedisConnection mockConnection = mock(RedisConnection.class);
        Properties mockInfo = new Properties();
        mockInfo.setProperty("used_memory_human", "100M");
        when(mockConnection.info("memory")).thenReturn(mockInfo);

        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenAnswer(invocation -> {
                    RedisCallback<?> callback = invocation.getArgument(0);
                    return callback.doInRedis(mockConnection);
                });

        // When & Then
        assertDoesNotThrow(() -> monitoringService.monitorCache());
    }

    @Test
    void getErrorRate_ShouldCalculateCorrectly() {
        // Given
        monitoringService.recordCacheAccess(true);   // operation
        monitoringService.recordCacheAccess(false);  // operation
        monitoringService.recordError();             // error

        // When
        double errorRate = monitoringService.getErrorRate();

        // Then
        assertEquals(0.5, errorRate, 0.01);
    }

    @Test
    void getErrorRate_ShouldHandleNoOperations() {
        // When
        double errorRate = monitoringService.getErrorRate();

        // Then
        assertEquals(0.0, errorRate);
    }

    @Test
    void monitorCache_ShouldLogMetrics() {
        // Given
        RedisConnection mockConnection = mock(RedisConnection.class);
        Properties mockInfo = new Properties();
        mockInfo.setProperty("redis_version", "6.0.0");
        mockInfo.setProperty("connected_clients", "1");
        when(mockConnection.info()).thenReturn(mockInfo);

        Properties mockMemoryInfo = new Properties();
        mockMemoryInfo.setProperty("used_memory_human", "100M");
        when(mockConnection.info("memory")).thenReturn(mockMemoryInfo);

        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenAnswer(invocation -> {
                    RedisCallback<?> callback = invocation.getArgument(0);
                    return callback.doInRedis(mockConnection);
                });

        // When
        monitoringService.recordCacheAccess(true);
        monitoringService.monitorCache();

        // Then - verify it completes without exception
        assertEquals(1.0, monitoringService.getHitRate());
    }
}