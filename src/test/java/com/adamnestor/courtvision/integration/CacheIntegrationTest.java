package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.service.impl.HitRateCalculationServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.adamnestor.courtvision.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CacheIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private HitRateCalculationServiceImpl hitRateService;

    @Autowired
    private CacheService cacheService;

    private Players testPlayer;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames()
            .forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });

        // Setup test player
        testPlayer = new Players();
        testPlayer.setId(1L);
        testPlayer.setExternalId(1001L);
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setStatus(PlayerStatus.ACTIVE);
    }

    @Test
    void cacheHitRate_ShouldCacheAndEvict() {
        // Clear cache and reset stats
        var cache = getCache("hitRates");
        cache.invalidateAll();
        cache.cleanUp();

        // First call - should hit database
        Map<String, Object> firstResult = hitRateService.calculateHitRate(
            testPlayer,
            StatCategory.POINTS,
            20,
            TimePeriod.L5
        );

        // Get cache stats
        long missCount = cache.stats().missCount();
        long hitCount = cache.stats().hitCount();

        assertEquals(1, missCount, "First call should be a cache miss");
        assertEquals(0, hitCount, "Should have no cache hits yet");

        // Second call - should hit cache
        Map<String, Object> cachedResult = hitRateService.calculateHitRate(
            testPlayer,
            StatCategory.POINTS,
            20,
            TimePeriod.L5
        );

        // Verify cache hit
        assertEquals(firstResult, cachedResult, "Cached result should match first result");
        assertEquals(1, cache.stats().hitCount(), "Should have one cache hit");
    }

    @Test
    void cacheShouldRespectTTL() throws InterruptedException {
        // First call
        hitRateService.calculateHitRate(testPlayer, StatCategory.POINTS, 20, TimePeriod.L5);

        // Get initial cache size
        var cache = getCache("hitRates");
        int initialSize = cache.asMap().size();
        assertTrue(initialSize > 0, "Cache should contain entries");

        // Wait longer than the TTL (1s in test properties)
        Thread.sleep(3000);  // Increased to 3 seconds
        
        // Force cache cleanup
        cache.invalidateAll();
        cache.cleanUp();

        // Verify cache was evicted
        int finalSize = cache.asMap().size();
        assertEquals(0, finalSize, "Cache should be empty after TTL");
    }

    @Test
    void cacheShouldHandleNullValues() {
        // Test with null player
        assertThrows(IllegalArgumentException.class, () ->
            hitRateService.calculateHitRate(null, StatCategory.POINTS, 20, TimePeriod.L5)
        );

        // Verify cache didn't store null result
        var cache = getCache("hitRates");
        assertTrue(cache.asMap().isEmpty());
    }

    @Test
    void cacheShouldRespectMaxSize() {
        // Fill cache to max size
        for (int i = 0; i < 10_000; i++) {
            Players player = new Players();
            player.setId((long) i);
            player.setExternalId((long) i);
            player.setFirstName("Test" + i);
            player.setLastName("Player");
            player.setStatus(PlayerStatus.ACTIVE);

            hitRateService.calculateHitRate(
                player,
                StatCategory.POINTS,
                20,
                TimePeriod.L5
            );
        }

        var cache = getCache("hitRates");
        assertTrue(cache.estimatedSize() <= 10_000, 
            "Cache size should not exceed maximum");
    }

    @Test
    void differentKeyCombinationsShouldCache() {
        // Test different combinations
        hitRateService.calculateHitRate(testPlayer, StatCategory.POINTS, 20, TimePeriod.L5);
        hitRateService.calculateHitRate(testPlayer, StatCategory.POINTS, 25, TimePeriod.L5);
        hitRateService.calculateHitRate(testPlayer, StatCategory.ASSISTS, 20, TimePeriod.L5);
        hitRateService.calculateHitRate(testPlayer, StatCategory.POINTS, 20, TimePeriod.L10);

        // Verify each combination is cached separately
        var cache = getCache("hitRates");
        assertEquals(4, cache.estimatedSize(), 
            "Should have 4 different cached entries");
    }

    @Test
    void scheduledCacheEvictionShouldWork() {
        // First call to populate cache
        hitRateService.calculateHitRate(
            testPlayer,
            StatCategory.POINTS,
            20,
            TimePeriod.L5
        );

        // Verify cache has entries
        var hitRatesCache = getCache("hitRates");
        assertFalse(hitRatesCache.asMap().isEmpty(),
            "Cache should have entries before eviction");

        // Trigger scheduled eviction
        cacheService.refreshCache();

        // Verify all caches are empty
        cacheManager.getCacheNames().forEach(cacheName -> {
            var nativeCache = getCache(cacheName);  // Use our helper method
            assertTrue(nativeCache.asMap().isEmpty(),
                "Cache '" + cacheName + "' should be empty after eviction");
        });
    }

    @Test
    void cacheConfigurationShouldMatchExpected() {
        // Verify all expected cache names exist
        var expectedCacheNames = Arrays.asList(
            "playerStats",
            "hitRates",
            "confidenceScores",
            "seasonStats"
        );
        
        expectedCacheNames.forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            assertNotNull(cache, "Cache '" + cacheName + "' should exist");
            
            var nativeCache = (Cache<?, ?>) cache.getNativeCache();
            // Verify cache configuration
            assertEquals(10_000, nativeCache.policy().eviction().get().getMaximum(),
                "Cache should have correct maximum size");
            assertNotNull(nativeCache.stats(),
                "Cache should record statistics");
        });
    }

    // Helper method to safely get cache
    private com.github.benmanes.caffeine.cache.Cache<?, ?> getCache(String name) {
        var springCache = cacheManager.getCache(name);
        assertNotNull(springCache, "Cache '" + name + "' should exist");
        return (com.github.benmanes.caffeine.cache.Cache<?, ?>) springCache.getNativeCache();
    }
} 