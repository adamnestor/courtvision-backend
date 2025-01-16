package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.UserPicksRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PickResultServiceTest {

    @Mock
    private UserPicksRepository userPicksRepository;

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Captor
    private ArgumentCaptor<UserPicks> userPicksCaptor;

    private PickResultService pickResultService;

    private Players mockPlayer;
    private Games mockGame;
    private GameStats mockGameStats;

    @BeforeEach
    void setUp() {
        pickResultService = new PickResultService(userPicksRepository, gameStatsRepository);

        // Initialize common test objects
        mockPlayer = new Players();
        mockPlayer.setId(1L);

        mockGame = new Games();
        mockGame.setId(1L);
        mockGame.setGameDate(LocalDateTime.now().minusDays(1));

        mockGameStats = new GameStats();
        mockGameStats.setPlayer(mockPlayer);
        mockGameStats.setGame(mockGame);
        mockGameStats.setPoints(25);
        mockGameStats.setAssists(8);
        mockGameStats.setRebounds(12);
    }

    @Test
    void processPickResult_PointsSuccess() {
        // Arrange
        UserPicks pick = createUserPick(StatCategory.POINTS, 20);
        when(gameStatsRepository.findByPlayerAndGame(mockPlayer, mockGame))
                .thenReturn(Optional.of(mockGameStats));

        // Act
        pickResultService.processPickResult(pick);

        // Assert
        verify(userPicksRepository).save(userPicksCaptor.capture());
        UserPicks savedPick = userPicksCaptor.getValue();
        assertTrue(savedPick.getResult());
        assertEquals(25, savedPick.getResultValue());
    }

    @Test
    void processPickResult_PointsFail() {
        // Arrange
        UserPicks pick = createUserPick(StatCategory.POINTS, 30);
        when(gameStatsRepository.findByPlayerAndGame(mockPlayer, mockGame))
                .thenReturn(Optional.of(mockGameStats));

        // Act
        pickResultService.processPickResult(pick);

        // Assert
        verify(userPicksRepository).save(userPicksCaptor.capture());
        UserPicks savedPick = userPicksCaptor.getValue();
        assertFalse(savedPick.getResult());
        assertEquals(25, savedPick.getResultValue());
    }

    @Test
    void processPickResult_AssistsSuccess() {
        // Arrange
        UserPicks pick = createUserPick(StatCategory.ASSISTS, 6);
        when(gameStatsRepository.findByPlayerAndGame(mockPlayer, mockGame))
                .thenReturn(Optional.of(mockGameStats));

        // Act
        pickResultService.processPickResult(pick);

        // Assert
        verify(userPicksRepository).save(userPicksCaptor.capture());
        UserPicks savedPick = userPicksCaptor.getValue();
        assertTrue(savedPick.getResult());
        assertEquals(8, savedPick.getResultValue());
    }

    @Test
    void processPickResult_ReboundsSuccess() {
        // Arrange
        UserPicks pick = createUserPick(StatCategory.REBOUNDS, 10);
        when(gameStatsRepository.findByPlayerAndGame(mockPlayer, mockGame))
                .thenReturn(Optional.of(mockGameStats));

        // Act
        pickResultService.processPickResult(pick);

        // Assert
        verify(userPicksRepository).save(userPicksCaptor.capture());
        UserPicks savedPick = userPicksCaptor.getValue();
        assertTrue(savedPick.getResult());
        assertEquals(12, savedPick.getResultValue());
    }

    @Test
    void processPickResult_NoStatsFound() {
        // Arrange
        UserPicks pick = createUserPick(StatCategory.POINTS, 20);
        when(gameStatsRepository.findByPlayerAndGame(mockPlayer, mockGame))
                .thenReturn(Optional.empty());

        // Act
        pickResultService.processPickResult(pick);

        // Assert
        verify(userPicksRepository, never()).save(any());
    }

    @Test
    void processResultsForDate_Success() {
        // Arrange
        LocalDate testDate = LocalDate.now().minusDays(1);
        List<UserPicks> picks = Arrays.asList(
                createUserPick(StatCategory.POINTS, 20),
                createUserPick(StatCategory.ASSISTS, 6)
        );
        when(userPicksRepository.findPicksByGameDate(testDate)).thenReturn(picks);
        when(gameStatsRepository.findByPlayerAndGame(mockPlayer, mockGame))
                .thenReturn(Optional.of(mockGameStats));

        // Act
        pickResultService.processResultsForDate(testDate);

        // Assert
        verify(userPicksRepository, times(2)).save(any());
    }

    @Test
    void processParlayResults_AllSuccess() {
        // Arrange
        String parlayId = "PARLAY123";
        UserPicks pick1 = createUserPick(StatCategory.POINTS, 20);
        UserPicks pick2 = createUserPick(StatCategory.ASSISTS, 6);
        pick1.setParlayId(parlayId);
        pick2.setParlayId(parlayId);

        List<UserPicks> parlayPicks = Arrays.asList(pick1, pick2);
        when(userPicksRepository.findByParlayId(parlayId)).thenReturn(parlayPicks);
        when(gameStatsRepository.findByPlayerAndGame(mockPlayer, mockGame))
                .thenReturn(Optional.of(mockGameStats));

        // Act
        pickResultService.processPickResult(pick1);

        // Assert
        verify(userPicksRepository, atLeastOnce()).save(userPicksCaptor.capture());
        List<UserPicks> savedPicks = userPicksCaptor.getAllValues();
        assertTrue(savedPicks.stream().anyMatch(pick ->
                pick.getResult() != null && pick.getParlayId().equals(parlayId)));
    }

    @Test
    void processParlayResults_MixedResults() {
        // Arrange
        String parlayId = "PARLAY123";
        UserPicks pick1 = createUserPick(StatCategory.POINTS, 30); // Will fail
        UserPicks pick2 = createUserPick(StatCategory.ASSISTS, 6); // Would succeed
        pick1.setParlayId(parlayId);
        pick2.setParlayId(parlayId);

        List<UserPicks> parlayPicks = Arrays.asList(pick1, pick2);
        when(userPicksRepository.findByParlayId(parlayId)).thenReturn(parlayPicks);
        when(gameStatsRepository.findByPlayerAndGame(mockPlayer, mockGame))
                .thenReturn(Optional.of(mockGameStats));

        // Act
        pickResultService.processPickResult(pick1);

        // Assert
        verify(userPicksRepository, atLeastOnce()).save(userPicksCaptor.capture());
        List<UserPicks> savedPicks = userPicksCaptor.getAllValues();
        assertTrue(savedPicks.stream().anyMatch(pick ->
                !pick.getResult() && pick.getParlayId().equals(parlayId)));
    }

    private UserPicks createUserPick(StatCategory category, Integer threshold) {
        UserPicks pick = new UserPicks();
        pick.setPlayer(mockPlayer);
        pick.setGame(mockGame);
        pick.setCategory(category);
        pick.setThreshold(threshold);
        return pick;
    }
}