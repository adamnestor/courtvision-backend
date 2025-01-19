package com.adamnestor.courtvision.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "playerStats",
            "hitRates",
            "confidenceScores",
            "seasonStats"
        ));
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .recordStats()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(10_000));
            
        return cacheManager;
    }
} 