package com.adamnestor.courtvision.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.adamnestor.courtvision.service.cache.CacheWarmingService;
import com.adamnestor.courtvision.service.cache.CacheMonitoringService;

@Service
public class WarmingStrategyService {
    private static final Logger log = LoggerFactory.getLogger(WarmingStrategyService.class);
    
    @Autowired
    private CacheWarmingService cacheWarmingService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CacheMonitoringService monitoringService;

    public enum WarmingPriority {
        HIGH, MEDIUM, LOW
    }

    public void implementPriorityWarming() {
        log.info("Starting priority warming");
        try {
            // Warm today's games
            cacheWarmingService.warmTodaysGames();
            trackWarmingProgress("priority_games", 100);
            
            // Warm active players' stats
            cacheWarmingService.warmTodaysPlayerCache();
            trackWarmingProgress("priority_players", 100);
            
            log.info("Priority warming completed successfully");
        } catch (Exception e) {
            log.error("Error during priority warming", e);
            monitoringService.recordError();
            trackWarmingProgress("priority", -1); // -1 indicates error
        }
    }

    public void implementOptionalWarming() {
        log.info("Starting optional warming");
        try {
            // Warm historical data (last 7 days)
            LocalDate today = LocalDate.now();
            for (int i = 1; i <= 7; i++) {
                LocalDate date = today.minusDays(i);
                cacheWarmingService.warmHistoricalGames(date);
                trackWarmingProgress("optional_historical", (i * 100) / 7);
            }
            
            log.info("Optional warming completed successfully");
        } catch (Exception e) {
            log.error("Error during optional warming", e);
            monitoringService.recordError();
            trackWarmingProgress("optional", -1);
        }
    }

    private void trackWarmingProgress(String type, int progress) {
        try {
            String key = "warming:progress:" + type;
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("progress", progress);
            progressData.put("timestamp", LocalDateTime.now());
            progressData.put("status", progress >= 100 ? "COMPLETED" : 
                                     progress < 0 ? "ERROR" : "IN_PROGRESS");
            
            redisTemplate.opsForValue().set(key, progressData, 24, TimeUnit.HOURS);
            log.debug("Tracked warming progress - type: {}, progress: {}", type, progress);
        } catch (Exception e) {
            log.error("Error tracking warming progress", e);
            monitoringService.recordError();
        }
    }

    public void executeWarmingStrategy(WarmingPriority priority) {
        if (priority == null) return;
        
        try {
            switch (priority) {
                case HIGH:
                    warmRegularData();
                    break;
                case MEDIUM:
                    warmOptionalData();
                    break;
                case LOW:
                    // Skip warming for low priority
                    break;
            }
            monitoringService.recordCacheAccess(true);
        } catch (Exception e) {
            monitoringService.recordError();
        }
    }

    public void warmRegularData() {
        try {
            monitoringService.recordCacheAccess(true);
        } catch (Exception e) {
            monitoringService.recordError();
        }
    }

    public void warmOptionalData() {
        try {
            monitoringService.recordCacheAccess(true);
        } catch (Exception e) {
            monitoringService.recordError();
        }
    }
} 