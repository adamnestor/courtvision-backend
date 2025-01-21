package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.client.BallDontLieClient;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.GameService;
import com.adamnestor.courtvision.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheWarmingServiceTest {

    @Mock private BallDontLieClient apiClient;
    @Mock private GameService gameService;
    @Mock private StatsService statsService;
    @Mock private HitRateCalculationServiceImpl hitRateService;
    @Mock private PlayersRepository playersRepository;

    private CacheWarmingService cacheWarmingService;

    @BeforeEach
    void setUp() {
        cacheWarmingService = new CacheWarmingService(
            apiClient, gameService, statsService, hitRateService, playersRepository);
    }

    @Test
    void warmCache_WarmsGamesAndPlayerData() {
        // Arrange
        Games game = createTestGame();
        List<Games> todaysGames = List.of(game);
        List<Players> players = createTestPlayers();

        when(gameService.getTodaysGames()).thenReturn(todaysGames);
        when(playersRepository.findByTeamId(anyLong())).thenReturn(players);

        // Act
        cacheWarmingService.warmCache();

        // Assert
        verify(apiClient).getGames(LocalDate.now().plusDays(1));
        verify(statsService).getAndUpdateGameStats(game);
        verify(hitRateService, times(players.size() * 3)) // 3 stat categories per player
            .calculateHitRate(any(Players.class), any(), anyInt(), any());
    }

    private Games createTestGame() {
        Teams homeTeam = new Teams();
        homeTeam.setId(1L);
        Teams awayTeam = new Teams();
        awayTeam.setId(2L);

        Games game = new Games();
        game.setId(1L);
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        return game;
    }

    private List<Players> createTestPlayers() {
        Players player1 = new Players();
        player1.setId(1L);
        Players player2 = new Players();
        player2.setId(2L);
        return Arrays.asList(player1, player2);
    }
} 