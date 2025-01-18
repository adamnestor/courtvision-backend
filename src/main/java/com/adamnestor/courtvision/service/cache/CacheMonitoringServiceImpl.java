package com.adamnestor.courtvision.service.cache;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;

@Service
public class CacheMonitoringServiceImpl implements CacheMonitoringService {
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheMonitoringServiceImpl(MeterRegistry meterRegistry, RedisTemplate<String, Object> redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void recordError() {
        meterRegistry.counter("cache.errors").increment();
    }

    @Override
    public boolean performHealthCheck() {
        try {
            Boolean result = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                connection.serverCommands().info("memory");
                return true;
            });
            return result != null && result;
        } catch (Exception e) {
            recordError();
            return false;
        }
    }

    @Override
    public double getHitRate() {
        return 1.0;
    }

    @Override
    public double getErrorRate() {
        return 0.0;
    }

    @Override
    public void recordCacheAccess(boolean isHit) {
        if (isHit) {
            meterRegistry.counter("cache.hits").increment();
        } else {
            meterRegistry.counter("cache.misses").increment();
        }
    }

    @Override
    public boolean checkHealth() {
        return performHealthCheck();
    }
} 