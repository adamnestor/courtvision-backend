package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StatsCacheService {
    private static final Logger logger = LoggerFactory.getLogger(StatsCacheService.class);

    /**
     * Cache player's recent game stats
     */
    @Cacheable(value = "playerStats", key = "#player.id + '-' + #timePeriod")
    public List<GameStats> getPlayerStats(Players player, TimePeriod timePeriod) {
        logger.debug("Cache miss for player stats - Player: {}, Period: {}",
                player.getId(), timePeriod);
        // This will be replaced by repository call
        return null;
    }

    /**
     * Cache calculated hit rates
     */
    @Cacheable(value = "hitRates",
            key = "#player.id + '-' + #category + '-' + #threshold + '-' + #timePeriod")
    public Map<String, Object> getHitRate(Players player, StatCategory category,
                                          Integer threshold, TimePeriod timePeriod) {
        logger.debug("Cache miss for hit rate - Player: {}, Category: {}, Threshold: {}, Period: {}",
                player.getId(), category, threshold, timePeriod);
        // This will be replaced by calculation service
        return null;
    }

    /**
     * Clear all caches for a player
     */
    @CacheEvict(value = {"playerStats", "hitRates"},
            key = "#player.id + '-' + '*'")
    public void clearPlayerCaches(Players player) {
        logger.info("Cleared all caches for player: {}", player.getId());
    }

    /**
     * Structure cache key for consistent formatting
     */
    private String buildCacheKey(String... parts) {
        return String.join(":", parts);
    }
}