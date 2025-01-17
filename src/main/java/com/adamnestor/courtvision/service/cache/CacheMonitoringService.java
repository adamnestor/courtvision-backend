package com.adamnestor.courtvision.service.cache;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;
import java.util.Properties;

@Service
public class CacheMonitoringService {
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheMonitoringService(MeterRegistry meterRegistry, RedisTemplate<String, Object> redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
        initializeMetrics();
    }

    private void initializeMetrics() {
        meterRegistry.gauge("cache.memory.usage", 0.0);
        meterRegistry.gauge("cache.keys.total", 0.0);
        meterRegistry.gauge("cache.hit.rate", 0.0);
    }

    public void recordCacheAccess(boolean isHit) {
        if (isHit) {
            meterRegistry.counter("cache.hits").increment();
        } else {
            meterRegistry.counter("cache.misses").increment();
        }
    }

    public void recordError() {
        meterRegistry.counter("cache.errors").increment();
    }

    public boolean performHealthCheck() {
        try {
            Properties info = redisTemplate.execute((RedisCallback<Properties>) (RedisCallback<Properties>) connection -> 
                connection.serverCommands().info("memory"), true);
            Long keyCount = redisTemplate.execute((RedisCallback<Long>) (RedisCallback<Long>) connection -> 
                connection.serverCommands().dbSize(), true);
            long totalKeys = keyCount != null ? keyCount : 0L;

            if (info != null && info.getProperty("used_memory") != null) {
                meterRegistry.gauge("cache.memory.usage", 
                    Double.parseDouble(info.getProperty("used_memory")));
                meterRegistry.gauge("cache.keys.total", (double) totalKeys);
                meterRegistry.gauge("cache.hit.rate", getHitRate());
                return true;
            }
            return false;
        } catch (Exception e) {
            recordError();
            return false;
        }
    }

    public double getHitRate() {
        double hits = meterRegistry.counter("cache.hits").count();
        double misses = meterRegistry.counter("cache.misses").count();
        double total = hits + misses;
        return total == 0 ? 1.0 : (hits / total) * 100;
    }

    public double getErrorRate() {
        double errors = meterRegistry.counter("cache.errors").count();
        double operations = meterRegistry.counter("cache.hits").count() 
            + meterRegistry.counter("cache.misses").count();
        return operations == 0 ? 0.0 : errors / (operations + errors);
    }
}