package com.adamnestor.courtvision.test.cache;

import com.adamnestor.courtvision.config.CacheConfig;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.cache.CacheKeyGenerator;
import com.adamnestor.courtvision.service.cache.CacheMonitoringService;
import com.adamnestor.courtvision.service.cache.CacheWarmingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CacheWarmingServiceTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private GamesRepository gamesRepository;

    @Mock
    private PlayersRepository playersRepository;

    @Mock
    private CacheKeyGenerator keyGenerator;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CacheMonitoringService monitoringService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CacheWarmingService warmingService;

    private Players testPlayer1;
    private Players testPlayer2;
    private Games testGame;
    private List<GameStats> testStats;
    private final String TEST_KEY = "test:key";

    @BeforeEach
    void setUp() {
        testPlayer1 = createTestPlayer(1L);
        testPlayer2 = createTestPlayer(2L);
        testGame = createTestGame();
        testStats = createTestGameStats(testPlayer1);

        // Core Redis setup
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        warmingService = new CacheWarmingService(
                gameStatsRepository,
                gamesRepository,
                playersRepository,
                keyGenerator,
                redisTemplate,
                monitoringService
        );
    }

    @Test
    void warmTodaysPlayerCache_WithValidPlayers_ShouldWarmCache() {
        // Given
        List<Games> games = Collections.singletonList(testGame);
        List<Players> players = Arrays.asList(testPlayer1, testPlayer2);

        when(gamesRepository.findByGameDateAndStatus(any(LocalDate.class), eq(GameStatus.SCHEDULED)))
                .thenReturn(games);
        when(playersRepository.findByTeamIdInAndStatus(anySet(), eq(PlayerStatus.ACTIVE)))
                .thenReturn(players);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class)))
                .thenReturn(testStats);
        when(keyGenerator.playerStatsKey(any(), any())).thenReturn(TEST_KEY);
        when(keyGenerator.playerHitRatesKey(any(), any(), any(), any())).thenReturn(TEST_KEY);

        // When
        warmingService.warmTodaysPlayerCache();

        // Then
        verify(valueOperations, atLeastOnce()).set(
                eq(TEST_KEY),
                any(),
                anyLong(),
                eq(TimeUnit.HOURS)
        );
        verify(monitoringService, never()).recordError();
    }

    @Test
    void warmTodaysPlayerCache_WithNoPlayers_ShouldLogWarning() {
        // Given
        when(gamesRepository.findByGameDateAndStatus(any(LocalDate.class), eq(GameStatus.SCHEDULED)))
                .thenReturn(Collections.emptyList());
        // Since the service still calls findByTeamIdInAndStatus, we mock it to return empty
        when(playersRepository.findByTeamIdInAndStatus(anySet(), eq(PlayerStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        // When
        warmingService.warmTodaysPlayerCache();

        // Then
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        verify(monitoringService, never()).recordError();
    }

    @Test
    void warmTodaysPlayerCache_WithRepositoryError_ShouldRecordError() {
        // Given
        when(gamesRepository.findByGameDateAndStatus(any(LocalDate.class), eq(GameStatus.SCHEDULED)))
                .thenReturn(Collections.singletonList(testGame));
        when(playersRepository.findByTeamIdInAndStatus(anySet(), eq(PlayerStatus.ACTIVE)))
                .thenThrow(new RuntimeException("Test error")); // Move error to repository call

        // When
        warmingService.warmTodaysPlayerCache();

        // Then
        verify(monitoringService).recordError();
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void scheduledCacheWarming_ShouldWarmAllCaches() {
        // Given
        when(gamesRepository.findByGameDateAndStatus(any(LocalDate.class), eq(GameStatus.SCHEDULED)))
                .thenReturn(Collections.singletonList(testGame));
        when(playersRepository.findByTeamIdInAndStatus(anySet(), eq(PlayerStatus.ACTIVE)))
                .thenReturn(Collections.singletonList(testPlayer1));
        when(keyGenerator.todaysGamesKey()).thenReturn(TEST_KEY);
        when(keyGenerator.playerStatsKey(any(), any())).thenReturn(TEST_KEY);
        when(keyGenerator.playerHitRatesKey(any(), any(), any(), any())).thenReturn(TEST_KEY);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class)))
                .thenReturn(testStats);

        // When
        warmingService.scheduledCacheWarming();

        // Then
        verify(valueOperations, atLeastOnce()).set(
                eq(TEST_KEY),
                any(),
                anyLong(),
                eq(TimeUnit.HOURS)
        );
        verify(monitoringService, never()).recordError();
    }

    private Players createTestPlayer(Long id) {
        Players player = new Players();
        player.setId(id);
        player.setFirstName("Test");
        player.setLastName("Player" + id);
        return player;
    }

    private Games createTestGame() {
        Games game = new Games();
        game.setId(1L);
        game.setGameDate(LocalDateTime.now());
        game.setStatus(GameStatus.SCHEDULED);

        Teams homeTeam = new Teams();
        homeTeam.setId(1L);
        game.setHomeTeam(homeTeam);

        Teams awayTeam = new Teams();
        awayTeam.setId(2L);
        game.setAwayTeam(awayTeam);

        return game;
    }

    private List<GameStats> createTestGameStats(Players player) {
        GameStats stats = new GameStats();
        stats.setPlayer(player);
        stats.setGame(testGame);
        stats.setPoints(20);
        stats.setAssists(5);
        stats.setRebounds(8);
        return Collections.singletonList(stats);
    }
}