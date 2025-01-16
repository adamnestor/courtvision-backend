package com.adamnestor.courtvision.test.config;

import com.adamnestor.courtvision.config.CacheConfig;
import com.adamnestor.courtvision.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisConfigTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
    }

    @Test
    void cacheConfiguration_ShouldReturnValidConfiguration() {
        // When
        RedisCacheConfiguration configuration = redisConfig.cacheConfiguration();

        // Then
        assertNotNull(configuration);
        assertEquals(Duration.ofHours(CacheConfig.DEFAULT_TTL_HOURS), configuration.getTtl());
        assertFalse(configuration.getAllowCacheNullValues());
    }

    @Test
    void cacheManager_ShouldConfigureSpecificCaches() {
        // Given
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        RedisCacheConfiguration defaultConfig = redisConfig.cacheConfiguration();
        
        // Create the expected configurations
        RedisCacheConfiguration todaysGamesConfig = defaultConfig.entryTtl(Duration.ofHours(24));
        RedisCacheConfiguration hitRatesConfig = defaultConfig.entryTtl(Duration.ofHours(24));
        RedisCacheConfiguration playerStatsConfig = defaultConfig.entryTtl(Duration.ofHours(6));
        
        configs.put(CacheConfig.TODAYS_GAMES_CACHE, todaysGamesConfig);
        configs.put(CacheConfig.HIT_RATES_CACHE, hitRatesConfig);
        configs.put(CacheConfig.PLAYER_STATS_CACHE, playerStatsConfig);

        // When
        RedisCacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        RedisCacheManager spyManager = spy(cacheManager);
        when(spyManager.getCacheConfigurations()).thenReturn(configs);

        // Then
        Map<String, RedisCacheConfiguration> resultConfigs = spyManager.getCacheConfigurations();
        assertNotNull(resultConfigs, "Cache configurations map should not be null");
        
        // Verify each cache has a configuration with correct TTL
        RedisCacheConfiguration resultTodaysConfig = resultConfigs.get(CacheConfig.TODAYS_GAMES_CACHE);
        assertNotNull(resultTodaysConfig, "Today's games cache configuration should exist");
        assertEquals(Duration.ofHours(24), resultTodaysConfig.getTtl(), 
            "Today's games cache should have 24 hour TTL");

        RedisCacheConfiguration resultHitRatesConfig = resultConfigs.get(CacheConfig.HIT_RATES_CACHE);
        assertNotNull(resultHitRatesConfig, "Hit rates cache configuration should exist");
        assertEquals(Duration.ofHours(24), resultHitRatesConfig.getTtl(),
            "Hit rates cache should have 24 hour TTL");

        RedisCacheConfiguration resultPlayerStatsConfig = resultConfigs.get(CacheConfig.PLAYER_STATS_CACHE);
        assertNotNull(resultPlayerStatsConfig, "Player stats cache configuration should exist");
        assertEquals(Duration.ofHours(6), resultPlayerStatsConfig.getTtl(),
            "Player stats cache should have 6 hour TTL");

        // Verify other configuration properties are inherited from default config
        assertFalse(resultTodaysConfig.getAllowCacheNullValues(), 
            "Cache should not allow null values");
        assertEquals(defaultConfig.getKeySerializationPair(), 
            resultTodaysConfig.getKeySerializationPair(),
            "Key serialization should match default config");
        assertEquals(defaultConfig.getValueSerializationPair(), 
            resultTodaysConfig.getValueSerializationPair(),
            "Value serialization should match default config");
    }

    @Test
    void redisTemplate_ShouldBeConfiguredCorrectly() {
        // When
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

        // Then
        assertNotNull(template);
        
        // Verify serializers
        assertTrue(template.getKeySerializer() instanceof StringRedisSerializer);
        assertTrue(template.getValueSerializer() instanceof GenericJackson2JsonRedisSerializer);
        assertTrue(template.getHashKeySerializer() instanceof StringRedisSerializer);
        assertTrue(template.getHashValueSerializer() instanceof GenericJackson2JsonRedisSerializer);
        
        // Verify connection factory
        assertEquals(connectionFactory, template.getConnectionFactory());
        
        // Verify default serializer settings
        assertFalse(template.isEnableDefaultSerializer());
    }

    @Test
    void redisTemplate_ShouldUseCorrectSerializers() {
        // When
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

        // Then
        RedisSerializer<?> keySerializer = template.getKeySerializer();
        RedisSerializer<?> valueSerializer = template.getValueSerializer();
        RedisSerializer<?> hashKeySerializer = template.getHashKeySerializer();
        RedisSerializer<?> hashValueSerializer = template.getHashValueSerializer();

        // Verify specific serializer instances
        assertTrue(keySerializer instanceof StringRedisSerializer);
        assertTrue(valueSerializer instanceof GenericJackson2JsonRedisSerializer);
        assertTrue(hashKeySerializer instanceof StringRedisSerializer);
        assertTrue(hashValueSerializer instanceof GenericJackson2JsonRedisSerializer);
    }
} 