package com.adamnestor.courtvision.service;

import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.cache.CacheKeyGenerator;
import com.adamnestor.courtvision.service.cache.CacheWarmingService;

@ExtendWith(MockitoExtension.class)
class DailyRefreshServiceTest {

    @Mock
    private CacheWarmingService cacheWarmingService;
    
    @Mock
    private PlayersRepository playersRepository;
    
    @Mock
    private GameStatsRepository gameStatsRepository;
    
    @Mock
    private CacheKeyGenerator keyGenerator;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOps;
    
    @Mock
    private HitRateCalculationService hitRateCalculationService;

    private DailyRefreshService dailyRefreshService;

    @BeforeEach
    void setUp() {
        dailyRefreshService = new DailyRefreshService();
        ReflectionTestUtils.setField(dailyRefreshService, "cacheWarmingService", cacheWarmingService);
        ReflectionTestUtils.setField(dailyRefreshService, "playersRepository", playersRepository);
        ReflectionTestUtils.setField(dailyRefreshService, "gameStatsRepository", gameStatsRepository);
        ReflectionTestUtils.setField(dailyRefreshService, "keyGenerator", keyGenerator);
        ReflectionTestUtils.setField(dailyRefreshService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(dailyRefreshService, "hitRateCalculationService", hitRateCalculationService);
    }

    @Test
    void performDailyRefresh_SuccessfulExecution() {
        // Given
        Players player = new Players();
        List<Players> activePlayers = Collections.singletonList(player);
        when(playersRepository.findByStatus(PlayerStatus.ACTIVE))
            .thenReturn(activePlayers);
        
        Map<String, Object> hitRateResult = new HashMap<>();
        hitRateResult.put("hitRate", new BigDecimal("0.75"));
        when(hitRateCalculationService.calculateHitRate(any(), any(), any(), any()))
            .thenReturn(hitRateResult);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // When
        dailyRefreshService.performDailyRefresh();

        // Then
        verify(cacheWarmingService).warmTodaysGames();
        verify(playersRepository, times(2)).findByStatus(PlayerStatus.ACTIVE);
    }

    @Test
    void updatePlayerStats_SuccessfulExecution() {
        // Given
        Players player = new Players();
        player.setId(1L);
        List<Players> activePlayers = Collections.singletonList(player);
        List<GameStats> recentStats = new ArrayList<>();
        
        when(playersRepository.findByStatus(PlayerStatus.ACTIVE)).thenReturn(activePlayers);
        when(gameStatsRepository.findPlayerRecentGames(player)).thenReturn(recentStats);
        when(keyGenerator.playerStatsKey(player, TimePeriod.L20)).thenReturn("player:1:stats:L20");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // When
        dailyRefreshService.updatePlayerStats();

        // Then
        verify(valueOps).set("player:1:stats:L20", recentStats, 6, TimeUnit.HOURS);
    }

    @Test
    void updateHitRateCalculations_SuccessfulExecution() {
        // Given
        Players player = new Players();
        player.setId(1L);
        List<Players> activePlayers = Collections.singletonList(player);
        Map<String, Object> hitRateResult = new HashMap<>();
        hitRateResult.put("hitRate", new BigDecimal("0.75"));
        
        when(playersRepository.findByStatus(PlayerStatus.ACTIVE)).thenReturn(activePlayers);
        when(hitRateCalculationService.calculateHitRate(any(), any(), any(), any()))
            .thenReturn(hitRateResult);
        when(keyGenerator.hitRatesKey(any(), any(), any(), any()))
            .thenReturn("player:1:hitrate:points:20:L10");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // When
        dailyRefreshService.updateHitRateCalculations();

        // Then
        verify(valueOps, atLeastOnce()).set(
            anyString(),
            any(BigDecimal.class),
            eq(24L),
            eq(TimeUnit.HOURS)
        );
    }

    @Test
    void performDailyRefresh_HandlesException() {
        // Given
        doThrow(new RuntimeException("Test error"))
            .when(cacheWarmingService)
            .warmTodaysGames();

        // When
        dailyRefreshService.performDailyRefresh();

        // Then
        verify(cacheWarmingService).warmTodaysGames();
        verifyNoInteractions(playersRepository);
        verifyNoInteractions(hitRateCalculationService);
    }
} 