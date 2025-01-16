package com.adamnestor.courtvision.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.data.redis.connection.RedisConnection;
import org.mockito.Mockito;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class CacheHealthIndicatorTest {
    @InjectMocks
    private CacheHealthIndicator healthIndicator;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private CacheMetricsService metricsService;

    @Test
    void testHealthCheck() throws Exception {
        var connectionFactory = mock(RedisConnectionFactory.class);
        var connection = mock(RedisConnection.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        
        Builder builder = new Builder();
        healthIndicator.doHealthCheck(builder);
        Health health = builder.build();
        
        verify(redisTemplate).getConnectionFactory();
        verify(metricsService).recordMetrics();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void testRedisConnectionFailure() throws Exception {
        when(redisTemplate.getConnectionFactory())
            .thenThrow(new RuntimeException("Redis connection failed"));
        
        Builder builder = new Builder();
        healthIndicator.doHealthCheck(builder);
        Health health = builder.build();
        
        verify(redisTemplate).getConnectionFactory();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
} 