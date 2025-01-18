package com.adamnestor.courtvision.config;

/**
 * Central configuration for all caching-related constants and settings
 */
public interface CacheConfig {
    // Cache region names
    String TODAYS_GAMES_CACHE = "todaysGames";
    String HIT_RATES_CACHE = "hitRates";
    String PLAYER_STATS_CACHE = "playerStats";
    String RECENT_GAMES_CACHE = "recentGames";

    // TTL configurations (in hours)
    long DEFAULT_TTL_HOURS = 24;
    long PLAYER_STATS_TTL_HOURS = 6;
    long HIT_RATES_TTL_HOURS = 24;
    long RECENT_GAMES_TTL_HOURS = 12;

    // Cache warming configurations
    int WARM_BATCH_SIZE = 50;
    int MAX_CACHE_SIZE = 10000;
    int MAX_ENTRIES_PER_REGION = 5000;

    // Cache monitoring thresholds
    double MIN_HIT_RATE = 0.80;
    double ERROR_THRESHOLD = 0.05;
    long MAX_CACHE_MEMORY_MB = 512;

    // Retry configurations
    int MAX_RETRY_ATTEMPTS = 3;
    long RETRY_DELAY_MS = 1000;

    // Update schedule (ET)
    String DAILY_UPDATE_CRON = "0 0 4 * * *";
    String CACHE_MONITORING_CRON = "0 */5 * * * *";

    // Key prefixes
    String PLAYER_KEY_PREFIX = "player";
    String GAME_KEY_PREFIX = "game";
    String HITRATE_KEY_PREFIX = "hitrate";
    String STATS_KEY_PREFIX = "stats";

    // Report configurations
    long REPORT_TTL_DAYS = 7;
    String REPORT_KEY_PREFIX = "cache:report";

    String FAILURE_REPORT_KEY_PREFIX = "cache:failure";

    // Key format constants
    String KEY_SEPARATOR = ":";
    String WILDCARD = "*";
    
    // Cache operation constants
    int BATCH_SIZE = 100;
    long CACHE_LOCK_TIMEOUT = 30; // seconds
    
    // Monitoring thresholds
    double CRITICAL_ERROR_RATE = 0.10;
    int MAX_RETRY_BACKOFF = 5000; // milliseconds
}