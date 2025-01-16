package com.adamnestor.courtvision.monitoring;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.Map;
import java.util.HashMap;

@Component
public class CacheHealthIndicator extends AbstractHealthIndicator {
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        boolean redisConnected = checkRedisConnection();
        Map<String, Object> metrics = getHealthMetrics();
        
        if (redisConnected) {
            builder.up()
                   .withDetails(metrics);
        } else {
            Map<String, Object> errorDetails = new HashMap<>(metrics);
            errorDetails.put("error", "Redis connection check failed");
            builder.down()
                   .withDetails(errorDetails);
        }
    }

    private boolean checkRedisConnection() {
        try {
            var factory = redisTemplate.getConnectionFactory();
            if (factory == null) return false;
            
            try (var connection = factory.getConnection()) {
                return connection.ping() != null;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> getHealthMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("version", "1.0");
        metrics.put("description", "Redis Cache Health Check");
        return metrics;
    }
} 