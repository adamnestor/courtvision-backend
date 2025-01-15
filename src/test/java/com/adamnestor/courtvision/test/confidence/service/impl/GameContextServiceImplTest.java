package com.adamnestor.courtvision.test.confidence.service.impl;

import com.adamnestor.courtvision.confidence.model.GameContext;
import com.adamnestor.courtvision.confidence.service.impl.GameContextServiceImpl;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.AdvancedGameStatsRepository;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameContextServiceImplTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private AdvancedGameStatsRepository advancedStatsRepository;

    private GameContextServiceImpl gameContextService;
    private Players player;
    private Games game;
    private Teams homeTeam;
    private Teams awayTeam;

    @BeforeEach
    void setUp() {
        gameContextService = new GameContextServiceImpl(gameStatsRepository, advancedStatsRepository);

        homeTeam = new Teams();
        homeTeam.setId(1L);
        homeTeam.setAbbreviation("HOME");

        awayTeam = new Teams();
        awayTeam.setId(2L);
        awayTeam.setAbbreviation("AWAY");

        player = new Players();
        player.setId(1L);
        player.setTeam(homeTeam);

        game = new Games();
        game.setId(1L);
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setGameDate(LocalDateTime.now());
    }

    @Test
    void calculateGameContext_HomeTeam_CalculatesCorrectVenueImpact() {
        // Arrange
        mockRepositoriesWithDefaultValues();

        // Act
        GameContext result = gameContextService.calculateGameContext(
                player, game, StatCategory.POINTS, 15);

        // Assert
        // Home team factor is 1.00, should normalize to 100.00
        BigDecimal expected = new BigDecimal("100.00");
        assertEquals(expected, result.getVenueImpact());
    }

    @Test
    void calculateGameContext_AwayTeam_CalculatesCorrectVenueImpact() {
        // Arrange
        player = new Players();
        player.setId(1L);
        player.setTeam(awayTeam);

        game = new Games();
        game.setId(1L);
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setGameDate(LocalDateTime.now());

        mockRepositoriesWithDefaultValues();

        // Act
        GameContext result = gameContextService.calculateGameContext(
                player, game, StatCategory.POINTS, 15);

        // Assert
        // Away team factor is 0.98, should normalize to 98.00
        BigDecimal expected = new BigDecimal("98.00");
        assertEquals(expected, result.getVenueImpact());
    }

    @Test
    void calculateMatchupImpact_WithSufficientHistory_CalculatesCorrectRate() {
        // Arrange
        List<GameStats> matchupHistory = createMatchupHistory(5, true);
        when(gameStatsRepository.findGamesByDateRange(any(), any(), any()))
                .thenReturn(matchupHistory);
        mockRepositoriesWithDefaultValues();

        // Act
        GameContext result = gameContextService.calculateGameContext(
                player, game, StatCategory.POINTS, 15);

        // Assert
        assertTrue(result.getMatchupImpact().compareTo(new BigDecimal("50.00")) > 0);
    }

    @Test
    void calculateMatchupImpact_WithInsufficientHistory_ReturnsBaseline() {
        // Arrange
        List<GameStats> matchupHistory = createMatchupHistory(2, true);
        when(gameStatsRepository.findGamesByDateRange(any(), any(), any()))
                .thenReturn(matchupHistory);
        mockRepositoriesWithDefaultValues();

        // Act
        GameContext result = gameContextService.calculateGameContext(
                player, game, StatCategory.POINTS, 15);

        // Assert
        assertEquals(new BigDecimal("50.00"), result.getMatchupImpact());
    }

    @Test
    void metThreshold_ForDifferentCategories() {
        // Arrange
        List<GameStats> history = new ArrayList<>();

        // Create multiple historical games
        for (int i = 0; i < 4; i++) {  // Need at least MIN_GAMES_FOR_MATCHUP
            Games historicalGame = new Games();
            historicalGame.setId((long) (i + 100));  // Different ID from current game
            historicalGame.setHomeTeam(homeTeam);
            historicalGame.setAwayTeam(awayTeam);
            historicalGame.setGameDate(LocalDateTime.now().minusDays(i + 1));

            GameStats stats = new GameStats();
            stats.setPoints(20);        // Above 15 threshold
            stats.setAssists(8);        // Above 5 threshold
            stats.setRebounds(12);      // Above 10 threshold
            stats.setGame(historicalGame);
            stats.setPlayer(player);
            history.add(stats);
        }

        when(gameStatsRepository.findGamesByDateRange(any(), any(), any()))
                .thenReturn(history);
        mockRepositoriesWithDefaultValues();

        // Act & Assert
        GameContext pointsResult = gameContextService.calculateGameContext(
                player, game, StatCategory.POINTS, 15);
        GameContext assistsResult = gameContextService.calculateGameContext(
                player, game, StatCategory.ASSISTS, 5);
        GameContext reboundsResult = gameContextService.calculateGameContext(
                player, game, StatCategory.REBOUNDS, 10);

        // Calculate expected matchup impact (100% success rate)
        BigDecimal expectedMatchupImpact = new BigDecimal("100.00");

        assertEquals(expectedMatchupImpact, pointsResult.getMatchupImpact());
        assertEquals(expectedMatchupImpact, assistsResult.getMatchupImpact());
        assertEquals(expectedMatchupImpact, reboundsResult.getMatchupImpact());
    }

    @Test
    void calculateDefensiveImpact_WithNoData_ReturnsBaseline() {
        // Arrange
        when(advancedStatsRepository.findTeamAverageDefensiveRating(any(), any(), any()))
                .thenReturn(Optional.empty());

        // Mock pace data to not interfere
        when(advancedStatsRepository.findTeamAveragePace(any(), any(), any()))
                .thenReturn(Optional.of(100.0));

        // Mock games stats to not affect matchup impact
        when(gameStatsRepository.findGamesByDateRange(any(), any(), any()))
                .thenReturn(List.of());

        // Act
        GameContext result = gameContextService.calculateGameContext(
                player, game, StatCategory.POINTS, 15);

        // Assert
        BigDecimal expected = new BigDecimal("50.00");
        assertEquals(expected, result.getDefensiveImpact());  // Only check defensive impact
    }

    @Test
    void calculatePaceImpact_WithMissingData_ReturnsBaseline() {
        // Arrange
        when(advancedStatsRepository.findTeamAveragePace(eq(homeTeam), any(), any()))
                .thenReturn(Optional.empty());
        when(advancedStatsRepository.findTeamAveragePace(eq(awayTeam), any(), any()))
                .thenReturn(Optional.empty());

        // Still need to mock defensive rating to avoid affecting the overall result
        when(advancedStatsRepository.findTeamAverageDefensiveRating(any(), any(), any()))
                .thenReturn(Optional.of(100.0));

        // Act
        GameContext result = gameContextService.calculateGameContext(
                player, game, StatCategory.POINTS, 15);

        // Assert
        BigDecimal expected = new BigDecimal("50.00");
        assertEquals(expected, result.getPaceImpact());  // Only check pace impact
    }

    @Test
    void isFavorableContext_AboveThreshold_ReturnsTrue() {
        // Arrange
        List<GameStats> matchupHistory = createMatchupHistory(5, true);
        when(gameStatsRepository.findGamesByDateRange(any(), any(), any()))
                .thenReturn(matchupHistory);
        when(advancedStatsRepository.findTeamAverageDefensiveRating(any(), any(), any()))
                .thenReturn(Optional.of(95.0)); // Favorable defense
        when(advancedStatsRepository.findTeamAveragePace(any(), any(), any()))
                .thenReturn(Optional.of(102.0)); // Higher pace

        // Act
        boolean result = gameContextService.isFavorableContext(
                player, game, StatCategory.POINTS, 15);

        // Assert
        assertTrue(result);
    }

    @Test
    void isFavorableContext_BelowThreshold_ReturnsFalse() {
        // Arrange
        List<GameStats> matchupHistory = createMatchupHistory(5, false);
        when(gameStatsRepository.findGamesByDateRange(any(), any(), any()))
                .thenReturn(matchupHistory);
        when(advancedStatsRepository.findTeamAverageDefensiveRating(any(), any(), any()))
                .thenReturn(Optional.of(115.0)); // Unfavorable defense
        when(advancedStatsRepository.findTeamAveragePace(any(), any(), any()))
                .thenReturn(Optional.of(95.0)); // Lower pace

        // Act
        boolean result = gameContextService.isFavorableContext(
                player, game, StatCategory.POINTS, 15);

        // Assert
        assertFalse(result);
    }

    private List<GameStats> createMatchupHistory(int count, boolean successful) {
        List<GameStats> history = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GameStats stats = new GameStats();
            stats.setGame(game);
            if (successful) {
                stats.setPoints(20);
                stats.setAssists(8);
                stats.setRebounds(12);
            } else {
                stats.setPoints(10);
                stats.setAssists(3);
                stats.setRebounds(5);
            }
            history.add(stats);
        }
        return history;
    }

    private void mockRepositoriesWithDefaultValues() {
        when(advancedStatsRepository.findTeamAverageDefensiveRating(any(), any(), any()))
                .thenReturn(Optional.of(105.0));
        when(advancedStatsRepository.findTeamAveragePace(any(), any(), any()))
                .thenReturn(Optional.of(98.5));
    }
}