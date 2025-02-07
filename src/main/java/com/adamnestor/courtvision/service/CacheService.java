package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.cache.CacheSynchronizationService;
import com.adamnestor.courtvision.monitoring.CacheMetricsService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheService {
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private static final Set<String> refreshInProgress = ConcurrentHashMap.newKeySet();
    
    private final CacheSynchronizationService syncService;
    private final CacheManager cacheManager;
    private final CacheMetricsService metricsService;

    public CacheService(
            CacheSynchronizationService syncService,
            CacheManager cacheManager,
            CacheMetricsService metricsService) {
        this.syncService = syncService;
        this.cacheManager = cacheManager;
        this.metricsService = metricsService;
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "America/New_York")
    public void refreshCache() {
        syncService.coordinateCacheRefresh("daily", () -> {
            try {
                logger.info("Starting daily cache refresh");
                clearAllCaches();
                validateCacheState();
                logger.info("Daily cache refresh completed successfully");
            } catch (Exception e) {
                logger.error("Error during cache refresh: {}", e.getMessage(), e);
                metricsService.recordError("all", "refresh_failure");
                attemptRecovery();
            }
        });
    }

    @CacheEvict(value = {
        "playerStats", 
        "hitRates", 
        "confidenceScores", 
        "seasonStats"
    }, allEntries = true)
    public void clearAllCaches() {
        logger.info("Clearing all caches");
    }

    public void refreshSingleCache(String cacheName) {
        if (!cacheManager.getCacheNames().contains(cacheName)) {
            throw new IllegalArgumentException("Invalid cache name: " + cacheName);
        }

        if (!refreshInProgress.add(cacheName)) {
            logger.warn("Refresh already in progress for cache: {}", cacheName);
            return;
        }

        try {
            syncService.executeCacheOperation("refresh:" + cacheName, () -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    validateCache(cacheName);
                    logger.info("Successfully refreshed cache: {}", cacheName);
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Error refreshing cache {}: {}", cacheName, e.getMessage(), e);
            metricsService.recordError(cacheName, "refresh_failure");
            attemptCacheRecovery(cacheName);
        } finally {
            refreshInProgress.remove(cacheName);
        }
    }

    private void validateCacheState() {
        cacheManager.getCacheNames().forEach(this::validateCache);
    }

    private void validateCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            logger.error("Cache not found: {}", cacheName);
            metricsService.recordError(cacheName, "cache_missing");
            return;
        }

        if (cache instanceof org.springframework.cache.caffeine.CaffeineCache caffeineCache) {
            var stats = caffeineCache.getNativeCache().stats();
            if (stats.hitRate() < 0.1 && stats.requestCount() > 100) {
                logger.warn("Cache {} showing poor performance - hit rate: {}", 
                    cacheName, stats.hitRate());
                metricsService.recordError(cacheName, "low_hit_rate");
            }
        }
    }

    private void attemptRecovery() {
        logger.info("Attempting cache recovery");
        cacheManager.getCacheNames().forEach(this::attemptCacheRecovery);
    }

    private void attemptCacheRecovery(String cacheName) {
        try {
            syncService.executeCacheOperation("recovery:" + cacheName, () -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    logger.info("Successfully recovered cache: {}", cacheName);
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Recovery failed for cache {}: {}", cacheName, e.getMessage(), e);
            metricsService.recordError(cacheName, "recovery_failure");
        }
    }

    public boolean needsRefresh(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache instanceof org.springframework.cache.caffeine.CaffeineCache caffeineCache) {
            var stats = caffeineCache.getNativeCache().stats();
            return stats.hitRate() < 0.1 && stats.requestCount() > 100;
        }
        return false;
    }
} 