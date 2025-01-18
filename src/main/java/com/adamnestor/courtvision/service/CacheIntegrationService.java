package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.adamnestor.courtvision.service.cache.CacheMonitoringService;

import java.time.LocalDateTime;
import java.util.*;

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

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY = 1000; // 1 second

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
            Set<String> playerKeys = redisTemplate.keys("player:stats:*");
            if (playerKeys == null || playerKeys.isEmpty()) {
                return true; // No cached data to check
            }

            for (String key : playerKeys) {
                Object cachedValue = redisTemplate.opsForValue().get(key);
                if (cachedValue instanceof List<?> cachedStats) {
                    // Compare with database
                    Long playerId = extractPlayerIdFromKey(key);
                    Players player = playersRepository.findById(playerId)
                        .orElseThrow(() -> new IllegalStateException("Player not found: " + playerId));
                    List<GameStats> dbStats = gameStatsRepository.findPlayerRecentGames(player);
                    
                    if (!compareGameStats(cachedStats, dbStats)) {
                        logger.error("Inconsistency found for player stats: {}", playerId);
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
            Set<String> hitRateKeys = redisTemplate.keys("player:hitrate:*");
            if (hitRateKeys == null || hitRateKeys.isEmpty()) {
                return true; // No cached data to check
            }

            for (String key : hitRateKeys) {
                Object cachedValue = redisTemplate.opsForValue().get(key);
                if (cachedValue instanceof Map<?, ?> hitRateData) {
                    // Verify hit rate calculation
                    if (!verifyHitRateCalculation(hitRateData)) {
                        logger.error("Invalid hit rate calculation found for key: {}", key);
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
        report.put("timestamp", LocalDateTime.now());
        report.put("totalKeys", redisTemplate.keys("*").size());
        report.put("hitRate", cacheMonitoringService.getHitRate());
        report.put("errorRate", cacheMonitoringService.getErrorRate());
        
        // Store report in Redis
        String reportKey = "cache:report:" + LocalDateTime.now();
        redisTemplate.opsForValue().set(reportKey, report, 7, java.util.concurrent.TimeUnit.DAYS);
        
        logger.info("Generated cache consistency report: {}", report);
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
        if (!cacheMonitoringService.performHealthCheck()) {
            throw new RuntimeException("Health check failed during retry");
        }
        dailyRefreshService.performDailyRefresh();
        warmingStrategyService.implementPriorityWarming();
    }

    private void retryPlayerStats() {
        dailyRefreshService.updatePlayerStats();
    }

    private void retryHitRates() {
        dailyRefreshService.updateHitRateCalculations();
    }

    private void reportFailure(String updateType) {
        Map<String, Object> failureReport = new HashMap<>();
        failureReport.put("updateType", updateType);
        failureReport.put("timestamp", LocalDateTime.now());
        failureReport.put("errorCount", cacheMonitoringService.getErrorRate());
        failureReport.put("healthStatus", cacheMonitoringService.performHealthCheck());
        failureReport.put("cacheMetrics", collectCacheMetrics());

        // Store failure report in Redis
        String failureKey = "cache:failure:" + updateType + ":" + LocalDateTime.now();
        redisTemplate.opsForValue().set(failureKey, failureReport, 30, java.util.concurrent.TimeUnit.DAYS);

        // Log detailed failure information
        logger.error("Update failure report generated: {}", failureReport);
    }

    private Map<String, Object> collectCacheMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("hitRate", cacheMonitoringService.getHitRate());
        metrics.put("errorRate", cacheMonitoringService.getErrorRate());
        metrics.put("totalKeys", redisTemplate.keys("*").size());
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
        // Check if key follows expected patterns
        return key.matches("^(player:(stats|hitrate):|cache:report:).*");
    }
} 