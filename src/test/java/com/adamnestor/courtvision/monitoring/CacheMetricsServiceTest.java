package com.adamnestor.courtvision.monitoring;

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

    @Test
    void testRecordMetrics_AllMetricsRecorded() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        metricsService.recordMetrics();
        verify(counter, times(2)).increment();
    }

    @Test
    void testRecordHitRatios_CounterIncremented() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        metricsService.recordHitRatios();
        verify(meterRegistry).counter("cache.hits");
        verify(counter, times(1)).increment();
    }

    @Test
    void testRecordMemoryUsage_CounterIncremented() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        metricsService.recordMemoryUsage();
        verify(meterRegistry).counter("cache.memory");
        verify(counter, times(1)).increment();
    }

    @Test
    void testRecordCacheSize_GaugeRegistered() {
        metricsService.recordCacheSize();
        verify(meterRegistry).gauge(eq("cache.size"), eq(0));
    }

    @Test
    void testRecordResponseTimes_GaugeRegistered() {
        metricsService.recordResponseTimes();
        verify(meterRegistry).gauge(eq("cache.response.time"), eq(-1));
    }

    @Test
    void testMetricsRegistration_CorrectMetricNames() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        metricsService.recordMetrics();
        verify(meterRegistry).counter("cache.hits");
        verify(meterRegistry).counter("cache.memory");
        verify(meterRegistry).gauge(eq("cache.size"), eq(0));
        verify(meterRegistry).gauge(eq("cache.response.time"), eq(-1));
    }
} 