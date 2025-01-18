package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.service.CacheIntegrationService;
import com.adamnestor.courtvision.service.DailyRefreshService;
import com.adamnestor.courtvision.service.WarmingStrategyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CacheIntegrationTest {

    @Autowired
    private CacheIntegrationService cacheIntegrationService;

    @Autowired
    private DailyRefreshService dailyRefreshService;

    @Autowired
    private WarmingStrategyService warmingStrategyService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testDailyRefreshProcess() {
        // Execute daily refresh
        dailyRefreshService.performDailyRefresh();

        // Verify cache state
        assertTrue(cacheIntegrationService.verifyDataSynchronization());
    }

    @Test
    void testCacheWarmingStrategy() {
        // Test priority warming
        warmingStrategyService.executeWarmingStrategy(
            WarmingStrategyService.WarmingPriority.HIGH
        );

        // Verify cache contains expected data
        assertTrue(redisTemplate.hasKey("player:stats:*"));
    }

    @Test
    void testErrorRecoveryMechanism() {
        // Simulate failure and test recovery
        cacheIntegrationService.handleUpdateFailure("daily-update");

        // Verify system recovered properly
        assertTrue(cacheIntegrationService.verifyDataSynchronization());
    }
} 