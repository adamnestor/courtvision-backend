package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.client.BallDontLieClient;
import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.AdvancedGameStats;
import com.adamnestor.courtvision.service.GameService;
import com.adamnestor.courtvision.service.StatsService;
import com.adamnestor.courtvision.service.AdvancedStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataRefreshServiceImplTest {

    @Mock private BallDontLieClient apiClient;
    @Mock private GameService gameService;
    @Mock private StatsService statsService;
    @Mock private AdvancedStatsService advancedStatsService;

    private DataRefreshServiceImpl dataRefreshService;

    @BeforeEach
    void setUp() {
        dataRefreshService = new DataRefreshServiceImpl(
            apiClient, gameService, statsService, advancedStatsService);
    }

    @Test
    void processGameResults_WhenFinalGame_ProcessesSuccessfully() {
        // Arrange
        ApiGame finalGame = new ApiGame();
        finalGame.setId(1L);
        finalGame.setStatus("Final");
        finalGame.setDate(LocalDate.now());

        Games mockGame = mock(Games.class);
        GameStats mockStats = createMockGameStats(mockGame);
        List<GameStats> basicStats = List.of(mockStats);
        List<AdvancedGameStats> advancedStats = List.of(mock(AdvancedGameStats.class));

        when(apiClient.getGames(any(LocalDate.class))).thenReturn(List.of(finalGame));
        when(gameService.findByExternalId(1L)).thenReturn(mockGame);
        when(statsService.getAndUpdateGameStats(mockGame)).thenReturn(basicStats);
        when(advancedStatsService.getAndUpdateGameAdvancedStats(mockGame)).thenReturn(advancedStats);

        // Act
        dataRefreshService.updateGameResults();

        // Assert
        verify(statsService).getAndUpdateGameStats(mockGame);
        verify(advancedStatsService).getAndUpdateGameAdvancedStats(mockGame);
    }

    @Test
    void processGameResults_WhenMissingAdvancedStats_ResyncsData() {
        // Arrange
        ApiGame finalGame = new ApiGame();
        finalGame.setId(1L);
        finalGame.setStatus("Final");
        finalGame.setDate(LocalDate.now());

        Games mockGame = mock(Games.class);
        when(mockGame.getId()).thenReturn(1L);

        GameStats mockStats = createMockGameStats(mockGame);
        List<GameStats> basicStats = List.of(mockStats);

        when(gameService.findByExternalId(1L)).thenReturn(mockGame);
        when(statsService.getAndUpdateGameStats(mockGame)).thenReturn(basicStats);
        when(advancedStatsService.getAndUpdateGameAdvancedStats(mockGame)).thenReturn(null);
        when(apiClient.getGames(any())).thenReturn(List.of(finalGame));

        // Act
        dataRefreshService.updateGameResults();

        // Assert
        verify(statsService, times(2)).getAndUpdateGameStats(mockGame);
        verify(advancedStatsService, times(2)).getAndUpdateGameAdvancedStats(mockGame);
    }

    @Test
    void processGameResults_WhenInvalidPlayerMapping_ResyncsData() {
        // Arrange
        ApiGame finalGame = new ApiGame();
        finalGame.setId(1L);
        finalGame.setStatus("Final");
        finalGame.setDate(LocalDate.now());

        Games mockGame = mock(Games.class);
        when(mockGame.getId()).thenReturn(1L);

        GameStats invalidStats = createMockGameStats(mockGame);
        invalidStats.setPlayer(null); // Invalid player mapping
        List<GameStats> basicStats = List.of(invalidStats);
        List<AdvancedGameStats> advancedStats = List.of(mock(AdvancedGameStats.class));

        when(gameService.findByExternalId(1L)).thenReturn(mockGame);
        when(statsService.getAndUpdateGameStats(mockGame)).thenReturn(basicStats);
        when(advancedStatsService.getAndUpdateGameAdvancedStats(mockGame)).thenReturn(advancedStats);
        when(apiClient.getGames(any())).thenReturn(List.of(finalGame));

        // Act
        dataRefreshService.updateGameResults();

        // Assert
        verify(statsService, times(2)).getAndUpdateGameStats(mockGame);
        verify(advancedStatsService, times(2)).getAndUpdateGameAdvancedStats(mockGame);
    }

    private GameStats createMockGameStats(Games game) {
        GameStats stats = new GameStats();
        stats.setGame(game);
        
        Players player = new Players();
        player.setId(1L);
        player.setExternalId(1001L);
        stats.setPlayer(player);
        
        return stats;
    }
} 