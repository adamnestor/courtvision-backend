package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.confidence.model.RestImpact;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RestImpactServiceImplTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private GamesRepository gamesRepository;

    private RestImpactServiceImpl restImpactService;
    private Players testPlayer;
    private Teams testTeam;
    private Teams opposingTeam;

    @BeforeEach
    void setUp() {
        restImpactService = new RestImpactServiceImpl(gameStatsRepository, gamesRepository);

        // Setup test team
        testTeam = createTeam(1L, "Test Team");
        opposingTeam = createTeam(2L, "Opposing Team");

        // Setup test player
        testPlayer = createPlayer(1L, "Test", "Player", testTeam);
    }

    @Test
    void calculateRestImpact_NoPreviousGames_ReturnsDefaultImpact() {
        // Arrange
        Games currentGame = createGame(1L, LocalDate.now(), testTeam, opposingTeam, GameStatus.SCHEDULED);

        when(gameStatsRepository.findPlayerRecentGames(any()))
                .thenReturn(Collections.emptyList());

        // Act
        RestImpact result = restImpactService.calculateRestImpact(
                testPlayer, currentGame, StatCategory.POINTS);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.ONE, result.getMultiplier());
        assertEquals(currentGame.getGameDate(), result.getGameDate());
        assertNull(result.getDaysOfRest());
        verify(gameStatsRepository, atLeastOnce()).findPlayerRecentGames(testPlayer);
    }

    @Test
    void calculateRestImpact_WithTwoDaysRest_ReturnsCorrectMultiplier() {
        // Arrange
        LocalDate now = LocalDate.now();
        Games currentGame = createGame(2L, now, testTeam, opposingTeam, GameStatus.SCHEDULED);
        Games previousGame = createGame(1L, now.minusDays(2), testTeam, createTeam(3L, "Another Team"), GameStatus.FINAL);

        GameStats previousGameStats = createGameStats(previousGame, testPlayer, 20);

        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(Collections.singletonList(previousGameStats));

        // Act
        RestImpact result = restImpactService.calculateRestImpact(
                testPlayer, currentGame, StatCategory.POINTS);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getDaysOfRest());
        assertEquals(new BigDecimal("1.02"), result.getMultiplier());
        verify(gameStatsRepository, atLeastOnce()).findPlayerRecentGames(testPlayer);
    }

    @Test
    void getHistoricalRestPerformance_NoGames_ReturnsOne() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(Collections.emptyList());

        // Act
        BigDecimal result = restImpactService.getHistoricalRestPerformance(
                testPlayer, 2, StatCategory.POINTS);

        // Assert
        assertEquals(BigDecimal.ONE, result);
        verify(gameStatsRepository, atLeastOnce()).findPlayerRecentGames(testPlayer);
    }

    @Test
    void getHistoricalRestPerformance_WithMatchingRestGames_ReturnsAverage() {
        // Arrange
        LocalDate now = LocalDate.now();

        // Create sequential games with 2 days rest between each
        Games game1 = createGame(1L, now.minusDays(6), testTeam, createTeam(3L, "Team 3"), GameStatus.FINAL);    // oldest
        Games game2 = createGame(2L, now.minusDays(4), testTeam, createTeam(4L, "Team 4"), GameStatus.FINAL);    // middle
        Games game3 = createGame(3L, now.minusDays(2), testTeam, createTeam(5L, "Team 5"), GameStatus.FINAL);    // newest

        GameStats stats1 = createGameStats(game1, testPlayer, 20);
        GameStats stats2 = createGameStats(game2, testPlayer, 30);  // After 2 days rest
        GameStats stats3 = createGameStats(game3, testPlayer, 40);  // After 2 days rest

        // Return games in newest-to-oldest order to match real repository behavior
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(Arrays.asList(stats3, stats2, stats1));

        // Act
        BigDecimal result = restImpactService.getHistoricalRestPerformance(
                testPlayer, 2, StatCategory.POINTS);

        // Assert
        // Game2: 30 points (after 2 days rest from game1)
        // Game3: 40 points (after 2 days rest from game2)
        // Average = (30 + 40) / 2 = 35
        assertEquals(new BigDecimal("35.0000"), result,
                "Should average points for games with 2 days rest: game2(30) and game3(40)");
        verify(gameStatsRepository, atLeastOnce()).findPlayerRecentGames(testPlayer);
    }

    @Test
    void analyzeRecentRestPattern_NoGames_ReturnsEmptyList() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(Collections.emptyList());

        // Act
        List<RestImpact> result = restImpactService.analyzeRecentRestPattern(
                testPlayer, StatCategory.POINTS);

        // Assert
        assertTrue(result.isEmpty());
        verify(gameStatsRepository, atLeastOnce()).findPlayerRecentGames(testPlayer);
    }

    @Test
    void analyzeRecentRestPattern_WithConsistentRestDays_ReturnsPattern() {
        // Arrange
        LocalDate now = LocalDate.now();

        // Create sequential games with dates relative to now
        Games game1 = createGame(97L, now.minusDays(6), testTeam, createTeam(3L, "Team 3"), GameStatus.FINAL);    // oldest
        Games game2 = createGame(98L, now.minusDays(4), testTeam, createTeam(4L, "Team 4"), GameStatus.FINAL);    // middle
        Games game3 = createGame(99L, now.minusDays(2), testTeam, createTeam(5L, "Team 5"), GameStatus.FINAL);    // newest

        GameStats stats1 = createGameStats(game1, testPlayer, 20);
        GameStats stats2 = createGameStats(game2, testPlayer, 24);
        GameStats stats3 = createGameStats(game3, testPlayer, 28);

        // Return games in newest-to-oldest order to match real repository behavior
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(Arrays.asList(stats1, stats2, stats3));

        // Act
        List<RestImpact> patterns = restImpactService.analyzeRecentRestPattern(
                testPlayer, StatCategory.POINTS);

        // Assert
        assertEquals(2, patterns.size(), "Should find two rest patterns");

        // First pattern (Game2 compared to Game1)
        assertEquals(2, patterns.get(0).getDaysOfRest(), "First rest period should be 2 days");
        assertEquals(game2.getGameDate(), patterns.get(0).getGameDate(),
                "First pattern should be for game2's date");

        // Second pattern (Game3 compared to Game2)
        assertEquals(2, patterns.get(1).getDaysOfRest(), "Second rest period should be 2 days");
        assertEquals(game3.getGameDate(), patterns.get(1).getGameDate(),
                "Second pattern should be for game3's date");
    }

    @Test
    void isBackToBack_NoAdjacentGames_ReturnsFalse() {
        // Arrange
        Games currentGame = createGame(100L, LocalDate.now(), testTeam, opposingTeam, GameStatus.SCHEDULED);

        when(gamesRepository.findByGameDateBetweenAndStatus(
                any(LocalDate.class), any(LocalDate.class), eq(GameStatus.FINAL)))
                .thenReturn(Collections.emptyList());

        // Act
        boolean result = restImpactService.isBackToBack(currentGame, testPlayer);

        // Assert
        assertFalse(result);
        verify(gamesRepository).findByGameDateBetweenAndStatus(
                any(LocalDate.class), any(LocalDate.class), eq(GameStatus.FINAL));
    }

    @Test
    void isBackToBack_WithAdjacentGame_ReturnsTrue() {
        // Arrange
        LocalDate now = LocalDate.now();
        Games currentGame = createGame(100L, now, testTeam, opposingTeam, GameStatus.SCHEDULED);
        Games adjacentGame = createGame(99L, now.minusDays(1), testTeam, createTeam(3L, "Team 3"), GameStatus.FINAL);

        when(gamesRepository.findByGameDateBetweenAndStatus(
                any(LocalDate.class), any(LocalDate.class), eq(GameStatus.FINAL)))
                .thenReturn(Arrays.asList(adjacentGame, currentGame));

        // Act
        boolean result = restImpactService.isBackToBack(currentGame, testPlayer);

        // Assert
        assertTrue(result);
        verify(gamesRepository).findByGameDateBetweenAndStatus(
                any(LocalDate.class), any(LocalDate.class), eq(GameStatus.FINAL));
    }

    // Helper methods
    private Teams createTeam(Long id, String name) {
        Teams team = new Teams();
        team.setId(id);
        team.setName(name);
        return team;
    }

    private Players createPlayer(Long id, String firstName, String lastName, Teams team) {
        Players player = new Players();
        player.setId(id);
        player.setFirstName(firstName);
        player.setLastName(lastName);
        player.setTeam(team);
        return player;
    }

    private Games createGame(Long id, LocalDate gameDate, Teams homeTeam, Teams awayTeam, GameStatus status) {
        Games game = new Games();
        game.setId(id);
        game.setGameDate(gameDate);
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setStatus(status.name());
        game.setGameTime("7:00 PM ET");  // Set a default game time for testing
        return game;
    }

    private GameStats createGameStats(Games game, Players player, int points) {
        GameStats stats = new GameStats();
        stats.setGame(game);
        stats.setPlayer(player);
        stats.setPoints(points);
        return stats;
    }
}