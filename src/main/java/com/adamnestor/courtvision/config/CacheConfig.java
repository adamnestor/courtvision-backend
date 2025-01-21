package com.adamnestor.courtvision.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "playerStats",
            "hitRates",
            "confidenceScores",
            "seasonStats",
            "apiResponses"
        ));
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .recordStats()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .expireAfterAccess(12, TimeUnit.HOURS)
            .maximumSize(10_000)
            .removalListener((key, value, cause) -> 
                logger.debug("Cache entry removed: key={}, cause={}", key, cause)));
            
        return cacheManager;
    }
} 