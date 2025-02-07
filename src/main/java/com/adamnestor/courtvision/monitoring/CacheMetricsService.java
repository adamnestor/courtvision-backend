package com.adamnestor.courtvision.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CacheMetricsService {
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    public CacheMetricsService(CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
        initializeCacheMetrics();
    }

    private void initializeCacheMetrics() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                var nativeCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();
                meterRegistry.gauge("cache.size." + cacheName, nativeCache.estimatedSize());
                meterRegistry.gauge("cache.hitRate." + cacheName, nativeCache.stats().hitRate());
                meterRegistry.gauge("cache.missRate." + cacheName, nativeCache.stats().missRate());
            }
        });
    }

    @Scheduled(fixedRate = 60000) // Update metrics every minute
    public void updateMetrics() {
        initializeCacheMetrics();
    }
} 