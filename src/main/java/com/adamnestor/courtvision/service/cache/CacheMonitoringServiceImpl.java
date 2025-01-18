package com.adamnestor.courtvision.service.cache;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;
import java.util.Properties;

@Service
public class CacheMonitoringServiceImpl implements CacheMonitoringService {
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, Object> redisTemplate;
    private long totalHits = 0;
    private long totalMisses = 0;
    private long totalErrors = 0;
    private long totalOperations = 0;

    public CacheMonitoringServiceImpl(MeterRegistry meterRegistry, RedisTemplate<String, Object> redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
        initializeMetrics();
    }

    private void initializeMetrics() {
        meterRegistry.counter("cache.hits");
        meterRegistry.counter("cache.misses");
        meterRegistry.counter("cache.errors");
        meterRegistry.gauge("cache.hit.rate", 1.0);
        meterRegistry.gauge("cache.memory.usage", 0.0);
        meterRegistry.gauge("cache.keys.total", 0.0);
    }

    @Override
    public void recordCacheAccess(boolean isHit) {
        if (isHit) {
            meterRegistry.counter("cache.hits").increment();
            totalHits++;
        } else {
            meterRegistry.counter("cache.misses").increment();
            totalMisses++;
        }
        totalOperations++;
        updateHitRate();
    }

    @Override
    public void recordError(Exception e) {
        meterRegistry.counter("cache.errors").increment();
        totalErrors++;
    }

    @Override
    public boolean performHealthCheck() {
        try {
            return redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                Properties memoryInfo = connection.serverCommands().info("memory");
                if (memoryInfo == null || memoryInfo.getProperty("used_memory") == null) {
                    return false;
                }
                long usedMemory = Long.parseLong(memoryInfo.getProperty("used_memory"));
                Long dbSize = connection.serverCommands().dbSize();
                if (dbSize == null) {
                    return false;
                }
                
                meterRegistry.gauge("cache.memory.usage", usedMemory);
                meterRegistry.gauge("cache.keys.total", dbSize);
                return true;
            }, true) == Boolean.TRUE;
        } catch (Exception e) {
            recordError(e);
            return false;
        }
    }

    @Override
    public double getHitRate() {
        if (totalOperations == 0) {
            return 1.0;
        }
        return (double) totalHits / totalOperations * 100;
    }

    @Override
    public double getErrorRate() {
        if (totalOperations == 0) {
            return 0.0;
        }
        return (double) totalErrors / totalOperations;
    }

    @Override
    public double getMissRate() {
        if (totalOperations == 0) {
            return 0.0;
        }
        return (double) totalMisses / totalOperations * 100;
    }

    private void updateHitRate() {
        double hitRate = getHitRate();
        meterRegistry.gauge("cache.hit.rate", hitRate);
    }

    @Override
    public boolean checkHealth() {
        return performHealthCheck();
    }
} 