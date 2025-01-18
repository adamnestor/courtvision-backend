package com.adamnestor.courtvision.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adamnestor.courtvision.service.cache.CacheWarmingService;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.service.cache.CacheKeyGenerator;
import org.springframework.data.redis.core.RedisTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class DailyRefreshService {
    
    private static final Logger log = LoggerFactory.getLogger(DailyRefreshService.class);
    
    @Autowired
    private CacheWarmingService cacheWarmingService;
    
    @Autowired
    private PlayersRepository playersRepository;
    
    @Autowired
    private GameStatsRepository gameStatsRepository;
    
    @Autowired
    private CacheKeyGenerator keyGenerator;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private HitRateCalculationService hitRateCalculationService;
    
    @Scheduled(cron = "0 0 4 * * *", zone = "America/New_York") // 4am ET
    public void performDailyRefresh() {
        log.info("Starting daily cache refresh");
        try {
            refreshTodaysGames();
            updatePlayerStats();
            updateHitRateCalculations();
            log.info("Daily cache refresh completed successfully");
        } catch (Exception e) {
            log.error("Error during daily cache refresh: {}", e.getMessage());
        }
    }
    
    protected void refreshTodaysGames() {
        cacheWarmingService.warmTodaysGames();
    }
    
    public void updatePlayerStats() {
        log.info("Starting incremental player stats update");
        try {
            List<Players> activePlayers = playersRepository.findByStatus(PlayerStatus.ACTIVE);
            int totalPlayers = activePlayers.size();
            int processed = 0;
            
            for (Players player : activePlayers) {
                try {
                    List<GameStats> recentStats = gameStatsRepository.findPlayerRecentGames(player);
                    String cacheKey = keyGenerator.playerStatsKey(player, TimePeriod.L20);
                    redisTemplate.opsForValue().set(cacheKey, recentStats, 6, TimeUnit.HOURS);
                    processed++;
                    
                    if (processed % 50 == 0) {
                        log.info("Processed {}/{} players", processed, totalPlayers);
                    }
                } catch (Exception e) {
                    log.error("Error updating stats for player {}: {}", player.getId(), e.getMessage());
                }
            }
            log.info("Completed player stats update. Processed {}/{} players", processed, totalPlayers);
        } catch (Exception e) {
            log.error("Error during player stats update", e);
            throw e;
        }
    }
    
    public void updateHitRateCalculations() {
        log.info("Starting hit rate calculations update");
        try {
            List<Players> activePlayers = playersRepository.findByStatus(PlayerStatus.ACTIVE);
            
            for (Players player : activePlayers) {
                try {
                    for (StatCategory category : StatCategory.values()) {
                        if (category == StatCategory.ALL) continue;
                        
                        List<Integer> thresholds = getThresholdsForCategory(category);
                        for (Integer threshold : thresholds) {
                            String cacheKey = keyGenerator.hitRatesKey(player, category, threshold, TimePeriod.L10);
                            Map<String, Object> hitRateResult = hitRateCalculationService.calculateHitRate(
                                player, category, threshold, TimePeriod.L10);
                            BigDecimal hitRate = (BigDecimal) hitRateResult.get("hitRate");
                            redisTemplate.opsForValue().set(cacheKey, hitRate, 24, TimeUnit.HOURS);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error calculating hit rates for player {}: {}", 
                        player.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during hit rate calculations update", e);
            throw e;
        }
    }
    
    private List<Integer> getThresholdsForCategory(StatCategory category) {
        return switch (category) {
            case POINTS -> Arrays.asList(10, 15, 20, 25);
            case ASSISTS -> Arrays.asList(2, 4, 6, 8);
            case REBOUNDS -> Arrays.asList(4, 6, 8, 10);
            default -> Collections.emptyList();
        };
    }
    
    /*
     * TODO: Future Development Methods
     * 
    private void fallbackToPreviousDay() {
        // TODO: Implement advanced error recovery
    }
    
    private void notifyAdmins() {
        // TODO: Implement external notification system
    }
    
    private void validateStats() {
        // TODO: Implement advanced data validation
    }
    
    private void validateHitRates() {
        // TODO: Implement advanced statistical validation
    }
    */
} 