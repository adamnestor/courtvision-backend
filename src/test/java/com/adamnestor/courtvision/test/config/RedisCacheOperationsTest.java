package com.adamnestor.courtvision.test.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class RedisCacheOperationsTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void testBasicCacheOperations() {
        // Test key pattern: player:{id}:stats
        String testKey = "player:1:stats";
        String testValue = "test data";

        // Write to cache
        redisTemplate.opsForValue().set(testKey, testValue);

        // Read from cache
        String retrieved = redisTemplate.opsForValue().get(testKey);

        assertThat(retrieved).isEqualTo(testValue);
    }
}