package com.adamnestor.courtvision.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CacheMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(CacheMetricsService.class);
    
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> operationTimers = new HashMap<>();
    private final Map<String, Counter> cacheErrors = new HashMap<>();
    private final CacheAlertConfig alertConfig;

    public CacheMetricsService(CacheManager cacheManager, MeterRegistry meterRegistry, CacheAlertConfig alertConfig) {
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
        this.alertConfig = alertConfig;
        initializeMetrics();
    }

    private void initializeMetrics() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (cache != null) {
                var nativeCache = cache.getNativeCache();

                // Basic metrics
                meterRegistry.gauge("cache.size", nativeCache, c -> c.estimatedSize());
                meterRegistry.gauge("cache.hitRate", nativeCache, c -> c.stats().hitRate());
                meterRegistry.gauge("cache.missRate", nativeCache, c -> c.stats().missRate());
                meterRegistry.gauge("cache.evictionCount", nativeCache, c -> c.stats().evictionCount());
                meterRegistry.gauge("cache.loadTime", nativeCache, c -> c.stats().averageLoadPenalty());

                // Operation timers
                operationTimers.put(cacheName, Timer.builder("cache.operation.time")
                    .tag("cache", cacheName)
                    .description("Cache operation timing")
                    .register(meterRegistry));

                // Error counters
                cacheErrors.put(cacheName, Counter.builder("cache.errors")
                    .tag("cache", cacheName)
                    .description("Cache operation errors")
                    .register(meterRegistry));
            }
        });
    }

    public void recordOperationTime(String cacheName, long timeInMs) {
        Timer timer = operationTimers.get(cacheName);
        if (timer != null) {
            timer.record(timeInMs, TimeUnit.MILLISECONDS);
            
            // Alert on slow operations
            if (timeInMs > alertConfig.getThresholds()
                    .getOrDefault(cacheName, new CacheAlertConfig.CacheThresholds())
                    .getOperationThresholdMs()) {
                logger.warn("Slow cache operation detected for {}: {}ms", cacheName, timeInMs);
            }
        }
    }

    public void recordError(String cacheName, String errorType) {
        Counter counter = cacheErrors.get(cacheName);
        if (counter != null) {
            counter.increment();
            logger.error("Cache error in {}: {}", cacheName, errorType);
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkCacheEffectiveness() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (cache != null) {
                var stats = cache.getNativeCache().stats();
                var thresholds = alertConfig.getThresholds()
                    .getOrDefault(cacheName, new CacheAlertConfig.CacheThresholds());

                // Check hit rate
                if (stats.hitRate() < thresholds.getHitRate()) {
                    logger.warn("Low hit rate for cache {}: {}", cacheName, stats.hitRate());
                }

                // Check eviction rate
                if (stats.evictionCount() > thresholds.getEvictionRate()) {
                    logger.warn("High eviction rate for cache {}: {}", cacheName, stats.evictionCount());
                }

                // Log performance metrics for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug("Cache {} metrics - Hit Rate: {}, Load Penalty: {}ms, Size: {}", 
                        cacheName,
                        String.format("%.2f", stats.hitRate()),
                        String.format("%.2f", stats.averageLoadPenalty()),
                        cache.getNativeCache().estimatedSize()
                    );
                }
            }
        });
    }
} 