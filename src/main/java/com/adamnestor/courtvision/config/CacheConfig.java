package com.adamnestor.courtvision.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(ExecutorService cacheExecutor) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAllowNullValues(false);
        
        // Register each cache with its specific configuration
        Arrays.asList(
            "apiResponses", "players", "teams", "gameStats", "advancedStats",
            "games", "playerStats", "playerGames", "hitRates", "confidenceScores",
            "statMaps", "todaysPlayers", "dashboardStats", "calculatedStats"
        ).forEach(cacheName -> {
            Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .recordStats()
                .executor(cacheExecutor);

            switch (cacheName) {
                case "apiResponses" -> cacheManager.registerCustomCache(cacheName, builder
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .maximumSize(100)
                    .build());
                case "players", "teams" -> cacheManager.registerCustomCache(cacheName, builder
                    .expireAfterWrite(24, TimeUnit.HOURS)
                    .maximumSize(1000)
                    .build());
                case "gameStats", "advancedStats" -> cacheManager.registerCustomCache(cacheName, builder
                    .expireAfterWrite(12, TimeUnit.HOURS)
                    .maximumSize(5000)
                    .build());
                case "confidenceScores", "hitRates" -> cacheManager.registerCustomCache(cacheName, builder
                    .expireAfterWrite(4, TimeUnit.HOURS)
                    .maximumSize(2000)
                    .scheduler(Scheduler.systemScheduler())
                    .build());
                case "todaysPlayers", "dashboardStats" -> cacheManager.registerCustomCache(cacheName, builder
                    .expireAfterWrite(30, TimeUnit.MINUTES)
                    .maximumSize(500)
                    .build());
                case "calculatedStats" -> cacheManager.registerCustomCache(cacheName, builder
                    .expireAfterWrite(6, TimeUnit.HOURS)
                    .maximumSize(3000)
                    .softValues()
                    .build());
                default -> cacheManager.registerCustomCache(cacheName, builder
                    .expireAfterWrite(24, TimeUnit.HOURS)
                    .maximumSize(1000)
                    .build());
            }
        });
        
        return cacheManager;
    }

    @Bean
    public ExecutorService cacheExecutor() {
        return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("cache-executor-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            }
        );
    }
} 