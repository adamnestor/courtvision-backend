package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.adamnestor.courtvision.service.cache.CacheMonitoringService;
import com.adamnestor.courtvision.config.CacheConfig;

import java.time.LocalDateTime;
import java.util.*;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Service
public class CacheIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(CacheIntegrationService.class);

    @Autowired
    private DailyRefreshService dailyRefreshService;
    
    @Autowired
    private WarmingStrategyService warmingStrategyService;
    
    @Autowired
    private CacheMonitoringService cacheMonitoringService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private GameStatsRepository gameStatsRepository;

    @Autowired
    private PlayersRepository playersRepository;

    private static final int MAX_RETRY_ATTEMPTS = CacheConfig.MAX_RETRY_ATTEMPTS;
    private static final long INITIAL_RETRY_DELAY = CacheConfig.RETRY_DELAY_MS;
    private static final long PLAYER_STATS_TTL_HOURS = CacheConfig.PLAYER_STATS_TTL_HOURS;
    private static final long HIT_RATES_TTL_HOURS = CacheConfig.HIT_RATES_TTL_HOURS;
    private static final long REPORT_TTL_DAYS = CacheConfig.REPORT_TTL_DAYS;

    public void performDailyUpdate() {
        logger.info("Starting daily cache update process");
        
        if (!cacheMonitoringService.performHealthCheck()) {
            logger.error("Health check failed, aborting daily update");
            return;
        }
        
        try {
            dailyRefreshService.performDailyRefresh();
            warmingStrategyService.implementPriorityWarming();
            verifyDataSynchronization();
            logger.info("Daily cache update completed successfully");
        } catch (Exception e) {
            logger.error("Error during daily cache update: {}", e.getMessage());
            handleUpdateFailure("daily-update");
        }
    }

    public boolean verifyDataSynchronization() {
        logger.info("Verifying data synchronization");
        try {
            boolean isConsistent = checkDataConsistency();
            generateReport();
            return isConsistent;
        } catch (Exception e) {
            logger.error("Error during data synchronization verification", e);
            return false;
        }
    }

    private boolean checkDataConsistency() {
        logger.debug("Performing data consistency checks");
        
        Map<String, Boolean> checks = new HashMap<>();
        
        // Check player stats consistency
        checks.put("player_stats", checkPlayerStatsConsistency());
        
        // Check hit rates consistency
        checks.put("hit_rates", checkHitRatesConsistency());
        
        // Check cache keys validity
        checks.put("cache_keys", checkCacheKeysValidity());
        
        // Log results
        checks.forEach((key, value) -> 
            logger.info("Consistency check for {}: {}", key, value ? "PASSED" : "FAILED")
        );
        
        return !checks.containsValue(false);
    }

    private boolean checkPlayerStatsConsistency() {
        try {
            Set<String> playerKeys = redisTemplate.keys(CacheConfig.PLAYER_KEY_PREFIX + ":" + 
                CacheConfig.STATS_KEY_PREFIX + ":*");
            if (playerKeys == null || playerKeys.isEmpty()) {
                return true;
            }

            for (String key : playerKeys) {
                Object cachedValue = redisTemplate.opsForValue().get(key);
                if (cachedValue instanceof List<?> cachedStats) {
                    Long playerId = extractPlayerIdFromKey(key);
                    Players player = playersRepository.findById(playerId)
                        .orElseThrow(() -> new IllegalStateException("Player not found: " + playerId));
                    List<GameStats> dbStats = gameStatsRepository.findPlayerRecentGames(player);
                    
                    if (!compareGameStats(cachedStats, dbStats)) {
                        logger.error("Inconsistency found for player stats: {}", playerId);
                        // Refresh the cache with correct data and TTL
                        redisTemplate.opsForValue().set(key, dbStats, PLAYER_STATS_TTL_HOURS, TimeUnit.HOURS);
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error checking player stats consistency: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkHitRatesConsistency() {
        try {
            Set<String> hitRateKeys = redisTemplate.keys(CacheConfig.PLAYER_KEY_PREFIX + ":" + CacheConfig.HITRATE_KEY_PREFIX + ":*");
            if (hitRateKeys == null || hitRateKeys.isEmpty()) {
                return true;
            }

            for (String key : hitRateKeys) {
                Object cachedValue = redisTemplate.opsForValue().get(key);
                if (cachedValue instanceof Map<?, ?> hitRateData) {
                    if (!verifyHitRateCalculation(hitRateData)) {
                        logger.error("Invalid hit rate calculation found for key: {}", key);
                        // Refresh the invalid hit rate with default values and proper TTL
                        redisTemplate.opsForValue().set(key, createDefaultHitRateData(), HIT_RATES_TTL_HOURS, TimeUnit.HOURS);
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error checking hit rates consistency: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> createDefaultHitRateData() {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("hitRate", 0.0);
        defaultData.put("average", 0.0);
        defaultData.put("gamesPlayed", 0);
        return defaultData;
    }

    private boolean checkCacheKeysValidity() {
        try {
            Set<String> allKeys = redisTemplate.keys("*");
            if (allKeys == null || allKeys.isEmpty()) {
                return true; // No keys to check
            }

            // Check key format and TTL
            for (String key : allKeys) {
                if (!isValidKeyFormat(key)) {
                    logger.error("Invalid key format found: {}", key);
                    return false;
                }

                Long ttl = redisTemplate.getExpire(key);
                if (ttl == null || ttl < 0) {
                    logger.error("Invalid TTL found for key: {}", key);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error checking cache keys validity: {}", e.getMessage());
            return false;
        }
    }

    private void generateReport() {
        Map<String, Object> report = new HashMap<>();
        long timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        report.put("timestamp", timestamp);
        
        // Store report in Redis
        String reportKey = CacheConfig.REPORT_KEY_PREFIX + ":" + timestamp;
        redisTemplate.opsForValue().set(reportKey, report, REPORT_TTL_DAYS, TimeUnit.DAYS);
    }

    public void handleUpdateFailure(String updateType) {
        logger.error("Handling update failure for: {}", updateType);
        try {
            retryUpdate(updateType);
            reportFailure(updateType);
        } catch (Exception e) {
            logger.error("Error handling update failure", e);
        }
    }

    private void retryUpdate(String updateType) {
        clearInvalidCache(updateType);
        logger.info("Starting retry process for: {}", updateType);
        int attempts = 0;
        long delay = INITIAL_RETRY_DELAY;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                attempts++;
                logger.info("Retry attempt {} for {}", attempts, updateType);
                
                switch (updateType) {
                    case "daily-update" -> retryDailyUpdate();
                    case "player-stats" -> retryPlayerStats();
                    case "hit-rates" -> retryHitRates();
                    default -> throw new IllegalArgumentException("Unknown update type: " + updateType);
                }
                
                logger.info("Retry successful for {}", updateType);
                return;
                
            } catch (Exception e) {
                logger.error("Retry attempt {} failed for {}: {}", attempts, updateType, e.getMessage());
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(delay);
                        delay *= 2; // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        logger.error("All retry attempts failed for {}", updateType);
        reportFailure(updateType);
    }

    private void retryDailyUpdate() {
        boolean success = false;
        try {
            if (!cacheMonitoringService.performHealthCheck()) {
                throw new RuntimeException("Health check failed during retry");
            }
            dailyRefreshService.performDailyRefresh();
            success = true;
        } finally {
            if (success) {
                try {
                    warmingStrategyService.implementPriorityWarming();
                } catch (Exception e) {
                    logger.error("Warming failed after successful refresh: {}", e.getMessage());
                }
            }
        }
    }

    private void retryPlayerStats() {
        dailyRefreshService.updatePlayerStats();
    }

    private void retryHitRates() {
        dailyRefreshService.updateHitRateCalculations();
    }

    private void reportFailure(String updateType) {
        Map<String, Object> failureReport = new HashMap<>();
        long timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        failureReport.put("timestamp", timestamp);
        failureReport.put("updateType", updateType);
        failureReport.put("errorCount", cacheMonitoringService.getErrorRate());
        failureReport.put("healthStatus", cacheMonitoringService.performHealthCheck());
        failureReport.put("cacheMetrics", collectCacheMetrics());

        // Store failure report in Redis
        String failureKey = CacheConfig.FAILURE_REPORT_KEY_PREFIX + ":" + updateType + ":" + timestamp;
        redisTemplate.opsForValue().set(failureKey, failureReport, REPORT_TTL_DAYS, TimeUnit.DAYS);

        // Log detailed failure information
        logger.error("Update failure report generated: {}", failureReport);
    }

    private Map<String, Object> collectCacheMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("hitRate", cacheMonitoringService.getHitRate());
        metrics.put("errorRate", cacheMonitoringService.getErrorRate());
        Set<String> keys = redisTemplate.keys("*");
        metrics.put("totalKeys", keys != null ? keys.size() : 0);
        return metrics;
    }

    private Long extractPlayerIdFromKey(String key) {
        try {
            return Long.parseLong(key.split(":")[2]);
        } catch (Exception e) {
            logger.error("Error extracting player ID from key: {}", key);
            return null;
        }
    }

    private boolean compareGameStats(List<?> cached, List<GameStats> db) {
        if (cached.size() != db.size()) return false;
        
        // Compare essential fields only
        for (int i = 0; i < cached.size(); i++) {
            if (!statsMatch(cached.get(i), db.get(i))) return false;
        }
        return true;
    }

    private boolean statsMatch(Object cached, GameStats db) {
        if (!(cached instanceof GameStats cachedStats)) return false;
        
        return cachedStats.getPoints() == db.getPoints() &&
               cachedStats.getAssists() == db.getAssists() &&
               cachedStats.getRebounds() == db.getRebounds();
    }

    private boolean verifyHitRateCalculation(Map<?, ?> hitRateData) {
        try {
            Number hitRate = (Number) hitRateData.get("hitRate");
            Number average = (Number) hitRateData.get("average");
            Number gamesPlayed = (Number) hitRateData.get("gamesPlayed");
            
            return hitRate != null && 
                   hitRate.doubleValue() >= 0 && 
                   hitRate.doubleValue() <= 100 &&
                   average != null && 
                   gamesPlayed != null;
        } catch (Exception e) {
            logger.error("Error verifying hit rate calculation: {}", e.getMessage());
            return false;
        }
    }

    private boolean isValidKeyFormat(String key) {
        return key.matches("^(" + 
            CacheConfig.PLAYER_KEY_PREFIX + ":(stats|hitrate):|" +
            "cache:report:|" +
            CacheConfig.HITRATE_KEY_PREFIX + ":|" +
            CacheConfig.GAME_KEY_PREFIX + "s:|" +
            "today:" + CacheConfig.GAME_KEY_PREFIX + "s:).*");
    }

    private void clearInvalidCache(String updateType) {
        try {
            switch (updateType) {
                case "daily-update" -> {
                    redisTemplate.delete(redisTemplate.keys(CacheConfig.PLAYER_KEY_PREFIX + ":" + CacheConfig.STATS_KEY_PREFIX + ":*"));
                    Set<String> gameKeys = redisTemplate.keys("today:" + CacheConfig.GAME_KEY_PREFIX + "s:*");
                    if (gameKeys != null && !gameKeys.isEmpty()) {
                        for (String key : gameKeys) {
                            Object value = redisTemplate.opsForValue().get(key);
                            if (value != null) {
                                redisTemplate.opsForValue().set(key, value, 
                                    CacheConfig.DEFAULT_TTL_HOURS, TimeUnit.HOURS);
                            }
                        }
                    }
                }
                case "player-stats" -> redisTemplate.delete(redisTemplate.keys(CacheConfig.PLAYER_KEY_PREFIX + ":" + CacheConfig.STATS_KEY_PREFIX + ":*"));
                case "hit-rates" -> redisTemplate.delete(redisTemplate.keys(CacheConfig.PLAYER_KEY_PREFIX + ":" + CacheConfig.HITRATE_KEY_PREFIX + ":*"));
            }
        } catch (Exception e) {
            logger.error("Failed to clear invalid cache for {}: {}", updateType, e.getMessage());
        }
    }
} 