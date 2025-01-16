package com.adamnestor.courtvision.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheMetricsServiceTest {
    @Mock
    private MeterRegistry meterRegistry;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private Counter counter;
    
    @InjectMocks
    private CacheMetricsService metricsService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
    }

    @Test
    void testRecordMetrics_AllMetricsRecorded() {
        metricsService.recordMetrics();
        verify(counter, times(4)).increment();
    }

    @Test
    void testRecordHitRatios_CounterIncremented() {
        metricsService.recordHitRatios();
        verify(meterRegistry).counter("cache.hits");
        verify(counter, times(1)).increment();
    }

    @Test
    void testRecordMemoryUsage_CounterIncremented() {
        metricsService.recordMemoryUsage();
        verify(meterRegistry).counter("cache.memory");
        verify(counter, times(1)).increment();
    }

    @Test
    void testRecordCacheSize_CounterIncremented() {
        metricsService.recordCacheSize();
        verify(meterRegistry).counter("cache.size");
        verify(counter, times(1)).increment();
    }

    @Test
    void testRecordResponseTimes_CounterIncremented() {
        metricsService.recordResponseTimes();
        verify(meterRegistry).counter("cache.response");
        verify(counter, times(1)).increment();
    }

    @Test
    void testMetricsRegistration_CorrectMetricNames() {
        metricsService.recordMetrics();
        verify(meterRegistry).counter("cache.hits");
        verify(meterRegistry).counter("cache.memory");
        verify(meterRegistry).counter("cache.size");
        verify(meterRegistry).counter("cache.response");
    }
} 