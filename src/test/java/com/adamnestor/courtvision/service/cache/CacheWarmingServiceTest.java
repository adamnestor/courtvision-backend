package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
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

@ExtendWith(MockitoExtension.class)
class CacheWarmingServiceTest {

    @Mock private GameStatsRepository gameStatsRepository;
    @Mock private GamesRepository gamesRepository;
    @Mock private PlayersRepository playersRepository;
    @Mock private CacheKeyGenerator keyGenerator;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private CacheMonitoringService monitoringService;
    @Mock private ValueOperations<String, Object> valueOperations;

    private CacheWarmingService warmingService;
    private Players testPlayer;
    private Games testGame;
    private List<GameStats> testStats;
    private Set<Long> teamIds;

    @BeforeEach
    void setUp() {
        testPlayer = createTestPlayer(1L);
        testGame = createTestGame();
        testStats = createTestGameStats(testPlayer);
        teamIds = new HashSet<>(Arrays.asList(testGame.getHomeTeam().getId(), testGame.getAwayTeam().getId()));

        warmingService = new CacheWarmingService(
            gameStatsRepository, gamesRepository, playersRepository,
            keyGenerator, redisTemplate, monitoringService
        );
    }

    @Test
    void warmTodaysGames_Success() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(gamesRepository.findByGameDateAndStatus(any(LocalDate.class), eq(GameStatus.SCHEDULED)))
            .thenReturn(Collections.singletonList(testGame));

        // When
        boolean result = warmingService.warmTodaysGames();

        // Then
        assertTrue(result);
        verify(monitoringService, never()).recordError();
    }

    @Test
    void warmTodaysPlayerCache_Success() {
        // Given
        List<Games> games = Collections.singletonList(testGame);
        List<Players> players = Collections.singletonList(testPlayer);
        String statsKey = "stats:key";
        String hitRatesKey = "hitrates:key";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(gamesRepository.findByGameDateAndStatus(any(LocalDate.class), eq(GameStatus.SCHEDULED)))
            .thenReturn(games);
        when(playersRepository.findByTeamIdInAndStatus(eq(teamIds), eq(PlayerStatus.ACTIVE)))
            .thenReturn(players);
        when(gameStatsRepository.findPlayerRecentGames(any(Players.class)))
            .thenReturn(testStats);
        when(keyGenerator.playerStatsKey(any(Players.class), any())).thenReturn(statsKey);
        when(keyGenerator.hitRatesKey(any(Players.class), any(), any(), any())).thenReturn(hitRatesKey);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // When
        warmingService.warmTodaysPlayerCache();

        // Then
        verify(valueOperations).set(eq(statsKey), eq(testStats), eq(6L), eq(TimeUnit.HOURS));
        verify(valueOperations, atLeastOnce()).set(
            eq(hitRatesKey), any(), eq(24L), eq(TimeUnit.HOURS)
        );
        verify(monitoringService, never()).recordError();
    }

    @Test
    void warmTodaysPlayerCache_NoGames() {
        // Given
        when(gamesRepository.findByGameDateAndStatus(any(LocalDate.class), eq(GameStatus.SCHEDULED)))
            .thenReturn(Collections.emptyList());

        // When
        warmingService.warmTodaysPlayerCache();

        // Then
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        verify(monitoringService, never()).recordError();
    }

    @Test
    void warmTodaysPlayerCache_RepositoryError() {
        // Given
        when(gamesRepository.findByGameDateAndStatus(any(LocalDate.class), eq(GameStatus.SCHEDULED)))
            .thenThrow(new RuntimeException("Database error"));

        // When
        warmingService.warmTodaysPlayerCache();

        // Then
        verify(monitoringService).recordError();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void scheduledCacheWarming_Success() {
        // Given
        String todaysGamesKey = "today:games";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(keyGenerator.todaysGamesKey()).thenReturn(todaysGamesKey);
        when(gamesRepository.findByGameDateAndStatus(any(LocalDate.class), eq(GameStatus.SCHEDULED)))
            .thenReturn(Collections.singletonList(testGame));

        // When
        warmingService.scheduledCacheWarming();

        // Then
        verify(valueOperations).set(eq(todaysGamesKey), any(), eq(24L), eq(TimeUnit.HOURS));
        verify(monitoringService, never()).recordError();
    }

    private Players createTestPlayer(Long id) {
        Players player = new Players();
        player.setId(id);
        player.setFirstName("Test");
        player.setLastName("Player");
        player.setStatus(PlayerStatus.ACTIVE);
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