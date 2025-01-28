package com.adamnestor.courtvision.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);
    private final ApplicationContext applicationContext;

    public CacheConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .recordStats()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .removalListener((key, value, cause) ->
                logger.debug("Cache entry removed: key={}, cause={}", key, cause))
            .maximumSize(1000);
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeine);
        cacheManager.setAllowNullValues(false);
        
        // Register all cache names used in the application
        cacheManager.setCacheNames(Arrays.asList(
            // API Response Caches
            "apiResponses",      // For BallDontLie API responses
            "players",          // For player data
            "teams",           // For team data
            "gameStats",       // For game statistics
            "advancedStats",   // For advanced statistics
            "games",           // For game data
            "playerStats",     // For player statistics

            // Calculation Caches
            "playerGames",        // Used in HitRateCalculationServiceImpl
            "hitRates",          // For hit rate calculations
            "confidenceScores",  // For confidence score calculations
            "statMaps",         // For stat map caching
            "todaysPlayers",    // For today's players list
            "dashboardStats",   // For dashboard statistics
            "calculatedStats"   // For calculated statistics
        ));
        
        return cacheManager;
    }

    @Scheduled(fixedRate = 30000)
    public void logCacheStatistics() {
        CacheManager cacheManager = applicationContext.getBean(CacheManager.class);
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                    ((CaffeineCache) cache).getNativeCache();
                logger.debug("CACHE_STATUS: name={}, size={}, keys=[{}]", 
                    cacheName, 
                    nativeCache.estimatedSize(),
                    String.join(", ", nativeCache.asMap().keySet().stream()
                        .map(key -> "'" + key + "'")
                        .collect(Collectors.toList()))
                );
            }
        });
    }
} 