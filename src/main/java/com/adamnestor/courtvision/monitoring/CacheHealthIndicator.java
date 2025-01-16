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
    private final CacheMetricsService metricsService;

    public CacheHealthIndicator(RedisTemplate<String, Object> redisTemplate, 
                              CacheMetricsService metricsService) {
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
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
        } catch (Exception e) {
            Map<String, Object> errorDetails = new HashMap<>(getHealthMetrics());
            errorDetails.put("error", e.getMessage());
            builder.down()
                   .withDetails(errorDetails);
        }
    }

    private boolean checkRedisConnection() {
        redisTemplate.getConnectionFactory().getConnection().ping();
        return true;
    }

    private Map<String, Object> getHealthMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("version", "1.0");
        metrics.put("description", "Redis Cache Health Check");
        return metrics;
    }
} 