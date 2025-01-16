package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.config.CacheConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class CacheMonitoringService {
    private static final Logger log = LoggerFactory.getLogger(CacheMonitoringService.class);

    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter cacheErrors;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheMonitoringService(MeterRegistry registry, RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.cacheHits = registry.counter("cache.hits");
        this.cacheMisses = registry.counter("cache.misses");
        this.cacheErrors = registry.counter("cache.errors");
    }

    /**
     * Records a cache access attempt
     */
    public void recordCacheAccess(boolean hit) {
        if (hit) {
            cacheHits.increment();
        } else {
            cacheMisses.increment();
        }
    }

    /**
     * Records a cache error
     */
    public void recordError() {
        cacheErrors.increment();
    }

    /**
     * Gets the current hit rate
     */
    public double getHitRate() {
        double hits = cacheHits.count();
        double total = hits + cacheMisses.count();
        return total == 0 ? 1.0 : hits / total;
    }

    /**
     * Scheduled task to monitor cache metrics
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorCache() {
        try {
            checkCacheHealth();
            checkCacheSize();
            logMetrics();
        } catch (Exception e) {
            log.error("Error monitoring cache: {}", e.getMessage());
            recordError();
        }
    }

    /**
     * Checks Redis connection and basic health
     */
    private void checkCacheHealth() {
        redisTemplate.execute((RedisConnection connection) -> {
            Properties info = connection.serverCommands().info();
            if (info != null) {
                log.info("Redis version: {}", info.getProperty("redis_version"));
                log.info("Connected clients: {}", info.getProperty("connected_clients"));
            } else {
                log.warn("Unable to retrieve Redis server info");
            }
            return null;
        });
    }

    /**
     * Monitors cache memory usage
     */
    private void checkCacheSize() {
        redisTemplate.execute((RedisConnection connection) -> {
            Properties info = connection.serverCommands().info("memory");
            if (info != null) {
                String memoryUsage = info.getProperty("used_memory_human", "0M")
                        .replaceAll("[^\\d.]", "");

                double usageMB = Double.parseDouble(memoryUsage);
                log.info("Current cache memory usage: {} MB", usageMB);

                if (usageMB > CacheConfig.MAX_CACHE_MEMORY_MB) {
                    log.warn("Cache memory usage exceeds threshold: {} MB", usageMB);
                }
            } else {
                log.warn("Unable to retrieve cache memory usage");
            }
            return null;
        });
    }

    /**
     * Logs cache performance metrics
     */
    private void logMetrics() {
        double hitRate = getHitRate();
        long totalOperations = getTotalOperations();
        double errorRate = getErrorRate();

        log.info("Cache Hit Rate: {}%", String.format("%.2f", hitRate * 100));
        log.info("Total Operations: {}", totalOperations);
        log.info("Error Rate: {}%", String.format("%.2f", errorRate * 100));

        if (hitRate < CacheConfig.MIN_HIT_RATE) {
            log.warn("Cache hit rate below threshold: {}%", String.format("%.2f", hitRate * 100));
        }
    }

    /**
     * Gets total number of operations
     */
    private long getTotalOperations() {
        return (long) (cacheHits.count() + cacheMisses.count());
    }

    /**
     * Gets the current error rate
     */
    public double getErrorRate() {
        long totalOps = getTotalOperations();
        return totalOps == 0 ? 0.0 : (cacheErrors.count() / (double) totalOps);
    }

    public boolean checkHealth() {
        // Implementation for health check
        return true;
    }
}