package com.adamnestor.courtvision.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;

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
        try {
            Set<String> keys = redisTemplate.keys("*");
            meterRegistry.gauge("cache.size", keys.size());
        } catch (Exception e) {
            logger.warn("Failed to record cache size", e);
            meterRegistry.gauge("cache.size", 0);
        }
    }

    public void recordResponseTimes() {
        logger.debug("Recording response times");
        long start = System.nanoTime();
        try {
            var factory = redisTemplate.getConnectionFactory();
            if (factory == null) {
                meterRegistry.gauge("cache.response.time", -1);
                return;
            }
            factory.getConnection().ping();
            double responseTime = (System.nanoTime() - start) / 1_000_000.0;
            meterRegistry.gauge("cache.response.time", responseTime);
        } catch (Exception e) {
            logger.warn("Failed to record response time", e);
            meterRegistry.gauge("cache.response.time", -1);
        }
    }
} 