package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import com.adamnestor.courtvision.service.DailyRefreshService;
import com.adamnestor.courtvision.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DataRefreshIntegrationTest {

    @Autowired
    private DailyRefreshService dailyRefreshService;

    @Autowired
    private PlayersRepository playersRepository;

    @Autowired
    private GameStatsRepository gameStatsRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Players testPlayer;

    @BeforeEach
    void setUp() {
        testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer = playersRepository.save(testPlayer);

        createTestGameStats();
    }

    @Test
    void testPlayerStatsUpdate() {
        dailyRefreshService.updatePlayerStats();

        String cacheKey = CacheConfig.PLAYER_KEY_PREFIX + ":" + CacheConfig.STATS_KEY_PREFIX + ":" + testPlayer.getId();
        Object cachedStats = redisTemplate.opsForValue().get(cacheKey);
        
        assertNotNull(cachedStats);
    }

    @Test
    void testHitRateRecalculation() {
        dailyRefreshService.updateHitRateCalculations();

        String cacheKey = CacheConfig.PLAYER_KEY_PREFIX + ":" + CacheConfig.HITRATE_KEY_PREFIX + ":" + testPlayer.getId();
        Object cachedHitRates = redisTemplate.opsForValue().get(cacheKey);
        
        assertNotNull(cachedHitRates);
    }

    @Test
    void testCacheTTLManagement() {
        dailyRefreshService.performDailyRefresh();

        String statsKey = CacheConfig.PLAYER_KEY_PREFIX + ":" + CacheConfig.STATS_KEY_PREFIX + ":" + testPlayer.getId();
        String hitRatesKey = CacheConfig.PLAYER_KEY_PREFIX + ":" + CacheConfig.HITRATE_KEY_PREFIX + ":" + testPlayer.getId();

        Long statsTTL = redisTemplate.getExpire(statsKey);
        Long hitRatesTTL = redisTemplate.getExpire(hitRatesKey);

        assertNotNull(statsTTL);
        assertNotNull(hitRatesTTL);
        assertTrue(statsTTL > 0);
        assertTrue(hitRatesTTL > 0);
    }

    private void createTestGameStats() {
        for (int i = 0; i < 10; i++) {
            Games game = new Games();
            game.setGameDate(LocalDate.now().minusDays(i + 1));
            game.setGameTime("7:00 PM ET");
            game.setStatus("FINAL");

            GameStats stats = new GameStats();
            stats.setPlayer(testPlayer);
            stats.setGame(game);
            stats.setPoints(20 + (i % 5));
            stats.setAssists(5 + (i % 3));
            stats.setRebounds(8 + (i % 4));
            
            gameStatsRepository.save(stats);
        }
    }
} 