package com.adamnestor.courtvision.config;

/**
 * Central configuration for all caching-related constants and settings
 */
public interface CacheConfig {
    // Cache region names
    String TODAYS_GAMES_CACHE = "todaysGames";
    String HIT_RATES_CACHE = "hitRates";
    String PLAYER_STATS_CACHE = "playerStats";

    // TTL configurations (in hours)
    long DEFAULT_TTL_HOURS = 24;
    long PLAYER_STATS_TTL_HOURS = 6;

    // Cache warming configurations
    int WARM_BATCH_SIZE = 50;
    int MAX_CACHE_SIZE = 10000;

    // Cache monitoring thresholds
    double MIN_HIT_RATE = 0.80; // 80% minimum hit rate
    long MAX_CACHE_MEMORY_MB = 512; // 512MB max cache size

    // Cache key prefixes
    String GAMES_PREFIX = "games";
    String PLAYER_PREFIX = "player";
    String HIT_RATE_PREFIX = "hitRate";
    String STATS_PREFIX = "stats";

    // Cache refresh timing (ET)
    int CACHE_REFRESH_HOUR = 4; // 4 AM ET
    int CACHE_REFRESH_MINUTE = 0;

    // Error thresholds
    int MAX_RETRY_ATTEMPTS = 3;
    long RETRY_DELAY_MS = 1000; // 1 second
}