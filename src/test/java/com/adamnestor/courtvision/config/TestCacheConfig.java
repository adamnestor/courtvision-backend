package com.adamnestor.courtvision.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestCacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new NoOpCacheManager();  // No-operation cache manager for tests
    }
} 