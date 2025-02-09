package com.adamnestor.courtvision.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Counter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.boot.actuate.health.Status;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Component
public class CacheMonitoringConfig implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(CacheMonitoringConfig.class);

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> cacheErrorCounters = new HashMap<>();
    private final CacheAlertConfig alertConfig;

    public CacheMonitoringConfig(CacheManager cacheManager, MeterRegistry meterRegistry, CacheAlertConfig alertConfig) {
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

                // Size metrics
                Gauge.builder("cache.size", nativeCache, c -> c.estimatedSize())
                    .tag("cache", cacheName)
                    .description("Current cache size")
                    .register(meterRegistry);

                // Hit rate metrics
                Gauge.builder("cache.hits", nativeCache, c -> c.stats().hitCount())
                    .tag("cache", cacheName)
                    .description("Cache hit count")
                    .register(meterRegistry);

                Gauge.builder("cache.misses", nativeCache, c -> c.stats().missCount())
                    .tag("cache", cacheName)
                    .description("Cache miss count")
                    .register(meterRegistry);

                // Eviction metrics
                Gauge.builder("cache.evictions", nativeCache, c -> c.stats().evictionCount())
                    .tag("cache", cacheName)
                    .description("Cache eviction count")
                    .register(meterRegistry);

                // Error counter
                cacheErrorCounters.put(cacheName, Counter.builder("cache.errors")
                    .tag("cache", cacheName)
                    .description("Cache operation errors")
                    .register(meterRegistry));
            }
        });
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Status status = Status.UP;

        for (String cacheName : cacheManager.getCacheNames()) {
            var cache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (cache != null) {
                var stats = cache.getNativeCache().stats();
                var thresholds = alertConfig.getThresholds()
                    .getOrDefault(cacheName, new CacheAlertConfig.CacheThresholds());
                
                double hitRate = stats.hitRate();
                long size = cache.getNativeCache().estimatedSize();
                long maxSize = cache.getNativeCache().policy().eviction()
                    .map(policy -> policy.getMaximum())
                    .orElse(1000L);
                
                details.put(cacheName + ".hitRate", hitRate);
                details.put(cacheName + ".size", size);
                details.put(cacheName + ".maxSize", maxSize);
                details.put(cacheName + ".evictionCount", stats.evictionCount());
                
                // Check against configured thresholds
                if (hitRate < thresholds.getHitRate() || 
                    size >= maxSize * thresholds.getSize()) {
                    status = Status.DOWN;
                    details.put(cacheName + ".status", "DEGRADED");
                } else {
                    details.put(cacheName + ".status", "HEALTHY");
                }
            }
        }

        return Health.status(status).withDetails(details).build();
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logCacheMetrics() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (cache != null) {
                var stats = cache.getNativeCache().stats();
                logger.info("Cache '{}' metrics - Size: {}, Hit Rate: {:.2f}, Miss Rate: {:.2f}, Evictions: {}",
                    cacheName,
                    cache.getNativeCache().estimatedSize(),
                    stats.hitRate(),
                    stats.missRate(),
                    stats.evictionCount()
                );

                // Alert on concerning metrics
                if (stats.hitRate() < 0.6) {
                    logger.warn("Low hit rate for cache '{}': {:.2f}", cacheName, stats.hitRate());
                }
            }
        });
    }

    public void recordCacheError(String cacheName, String errorType) {
        Counter counter = cacheErrorCounters.get(cacheName);
        if (counter != null) {
            counter.increment();
            logger.error("Cache error in '{}': {}", cacheName, errorType);
        }
    }
} 