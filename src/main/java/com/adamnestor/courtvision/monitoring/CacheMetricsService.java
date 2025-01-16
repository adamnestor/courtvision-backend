package com.adamnestor.courtvision.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class CacheMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(CacheMetricsService.class);
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheMetricsService(MeterRegistry meterRegistry, RedisTemplate<String, Object> redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 60000)
    public void recordMetrics() {
        recordHitRatios();
        recordMemoryUsage();
        recordCacheSize();
        recordResponseTimes();
    }

    public void recordHitRatios() {
        logger.debug("Recording hit ratios");
        meterRegistry.counter("cache.hits").increment();
    }

    public void recordMemoryUsage() {
        logger.debug("Recording memory usage");
        meterRegistry.counter("cache.memory").increment();
    }

    public void recordCacheSize() {
        logger.debug("Recording cache size");
        meterRegistry.counter("cache.size").increment();
    }

    public void recordResponseTimes() {
        logger.debug("Recording response times");
        meterRegistry.counter("cache.response").increment();
    }
} 