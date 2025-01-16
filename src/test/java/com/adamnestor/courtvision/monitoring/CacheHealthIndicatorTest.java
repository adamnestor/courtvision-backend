package com.adamnestor.courtvision.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheHealthIndicatorTest {
    @InjectMocks
    private CacheHealthIndicator healthIndicator;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private CacheMetricsService metricsService;

    @Test
    void healthCheck_WhenRedisIsConnected_ShouldReturnUp() throws Exception {
        // Given
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(null);

        Health.Builder builder = new Health.Builder();
        
        // When
        healthIndicator.doHealthCheck(builder);
        Health health = builder.build();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
            .containsEntry("version", "1.0")
            .containsEntry("description", "Redis Cache Health Check")
            .doesNotContainKey("error");
    }

    @Test
    void healthCheck_WhenRedisIsNotConnected_ShouldReturnDown() throws Exception {
        // Given
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        RuntimeException exception = new RuntimeException("Connection failed");
        when(connection.ping()).thenThrow(exception);

        Health.Builder builder = new Health.Builder();
        
        // When
        healthIndicator.doHealthCheck(builder);
        Health health = builder.build();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
            .containsEntry("version", "1.0")
            .containsEntry("description", "Redis Cache Health Check")
            .containsEntry("error", exception.getMessage());
    }

    @Test
    void healthCheck_WhenExceptionOccurs_ShouldReturnDown() throws Exception {
        // Given
        Health.Builder builder = new Health.Builder();
        RuntimeException exception = new RuntimeException("Redis connection failed");
        when(redisTemplate.getConnectionFactory()).thenThrow(exception);
        
        // When
        healthIndicator.doHealthCheck(builder);
        Health health = builder.build();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
            .containsEntry("version", "1.0")
            .containsEntry("description", "Redis Cache Health Check")
            .containsEntry("error", exception.getMessage());
    }
} 