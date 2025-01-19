package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.service.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.domain.PlayerStatus;
import com.github.benmanes.caffeine.cache.Cache;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

@SpringBootTest
class CacheServiceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private HitRateCalculationServiceImpl hitRateService;

    @Test
    void testCacheEviction() {
        // Create test player
        Players player = new Players();
        player.setId(1L);
        player.setExternalId(1001L);
        player.setFirstName("Test");
        player.setLastName("Player");
        player.setStatus(PlayerStatus.ACTIVE);

        // First call - should hit the database
        Map<String, Object> result = hitRateService.calculateHitRate(
            player,
            StatCategory.POINTS, 
            20, 
            TimePeriod.L5
        );

        // Second call - should hit the cache
        Map<String, Object> cachedResult = hitRateService.calculateHitRate(
            player,
            StatCategory.POINTS, 
            20, 
            TimePeriod.L5
        );

        // Verify cache hit
        assertEquals(result, cachedResult);

        // Clear cache
        cacheService.refreshCache();

        // Verify caches are empty
        var cache = cacheManager.getCache("hitRates");
        assertNotNull(cache, "Cache 'hitRates' should exist");
        assertTrue(((Cache<?, ?>)cache.getNativeCache()).asMap().isEmpty());
    }
} 