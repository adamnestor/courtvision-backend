package com.adamnestor.courtvision.confidence.service.impl;

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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BlowoutRiskServiceImplTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private AdvancedGameStatsRepository advancedStatsRepository;

    private BlowoutRiskServiceImpl blowoutRiskService;
    private Teams homeTeam;
    private Teams awayTeam;
    private Games game;

    @BeforeEach
    void setUp() {
        blowoutRiskService = new BlowoutRiskServiceImpl(gameStatsRepository, advancedStatsRepository);

        homeTeam = new Teams();
        homeTeam.setId(1L);
        homeTeam.setName("Home Team");

        awayTeam = new Teams();
        awayTeam.setId(2L);
        awayTeam.setName("Away Team");

        game = new Games();
        game.setId(1L);
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setGameDate(LocalDateTime.now());
    }

    @Test
    void calculateBlowoutRisk_WithStrongHomeTeamVsWeakTeam_ReturnsHighRisk() {
        // Arrange
        // Mock strong home team stats (high net rating, fast pace)
        when(advancedStatsRepository.findTeamGamesByDateRange(
                eq(homeTeam), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(createAdvancedGameStats(115.0, 102.0));

        // Mock weak away team stats (low net rating, slower pace)
        when(advancedStatsRepository.findTeamGamesByDateRange(
                eq(awayTeam), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(createAdvancedGameStats(95.0, 98.0));

        // Mock historical matchups showing blowout pattern
        when(gameStatsRepository.findGamesByTeams(
                eq(homeTeam), eq(awayTeam), any(LocalDate.class)))
                .thenReturn(createBlowoutGames());

        // Act
        BigDecimal risk = blowoutRiskService.calculateBlowoutRisk(game);
        System.out.println("Calculated Risk: " + risk);

        // Assert
        assertTrue(risk.compareTo(new BigDecimal("60.00")) > 0,
                "Risk should be high (>60%) for strong vs weak team matchup");
    }

    @Test
    void calculateBlowoutRisk_WithEvenlyMatchedTeams_ReturnsLowRisk() {
        // Arrange
        // Mock evenly matched team stats
        when(advancedStatsRepository.findTeamGamesByDateRange(
                eq(homeTeam), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(createAdvancedGameStats(105.0, 100.0));

        when(advancedStatsRepository.findTeamGamesByDateRange(
                eq(awayTeam), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(createAdvancedGameStats(103.0, 99.0));

        // Mock historical matchups showing competitive games
        when(gameStatsRepository.findGamesByTeams(
                eq(homeTeam), eq(awayTeam), any(LocalDate.class)))
                .thenReturn(createCompetitiveGames());

        // Act
        BigDecimal risk = blowoutRiskService.calculateBlowoutRisk(game);
        System.out.println("Calculated Risk: " + risk);

        // Assert
        assertTrue(risk.compareTo(new BigDecimal("40.00")) < 0,
                "Risk should be low (<40%) for evenly matched teams");
    }

    @Test
    void calculateBlowoutRisk_WithNoHistoricalData_ReturnsBaselineRisk() {
        // Arrange
        when(advancedStatsRepository.findTeamGamesByDateRange(
                any(Teams.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        when(gameStatsRepository.findGamesByTeams(
                any(Teams.class), any(Teams.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // Act
        BigDecimal risk = blowoutRiskService.calculateBlowoutRisk(game);

        // Assert
        assertNotNull(risk, "Risk should not be null even without historical data");
        assertTrue(risk.compareTo(BigDecimal.ZERO) >= 0,
                "Risk should not be negative");
        assertTrue(risk.compareTo(new BigDecimal("100")) <= 0,
                "Risk should not exceed 100%");
    }

    @Test
    void calculateBlowoutRisk_WithHighPaceDifferential_IncreasesRisk() {
        // Arrange
        // Mock high-pace home team vs low-pace away team
        when(advancedStatsRepository.findTeamGamesByDateRange(
                eq(homeTeam), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(createAdvancedGameStats(105.0, 110.0)); // High pace

        when(advancedStatsRepository.findTeamGamesByDateRange(
                eq(awayTeam), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(createAdvancedGameStats(103.0, 95.0)); // Low pace

        when(gameStatsRepository.findGamesByTeams(
                any(Teams.class), any(Teams.class), any(LocalDate.class)))
                .thenReturn(createCompetitiveGames());

        // Act
        BigDecimal risk = blowoutRiskService.calculateBlowoutRisk(game);

        // Get risk with similar pace for comparison
        when(advancedStatsRepository.findTeamGamesByDateRange(
                eq(homeTeam), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(createAdvancedGameStats(105.0, 100.0));
        BigDecimal normalPaceRisk = blowoutRiskService.calculateBlowoutRisk(game);

        // Assert
        assertTrue(risk.compareTo(normalPaceRisk) > 0,
                "Risk should be higher when there's a significant pace differential");
    }

    @Test
    void isHighBlowoutRisk_WithRiskAboveThreshold_ReturnsTrue() {
        // Arrange
        // Mock strong mismatch in team strengths
        when(advancedStatsRepository.findTeamGamesByDateRange(
                eq(homeTeam), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(createAdvancedGameStats(120.0, 102.0)); // Very strong

        when(advancedStatsRepository.findTeamGamesByDateRange(
                eq(awayTeam), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(createAdvancedGameStats(90.0, 98.0)); // Very weak

        when(gameStatsRepository.findGamesByTeams(
                eq(homeTeam), eq(awayTeam), any(LocalDate.class)))
                .thenReturn(createBlowoutGames());

        // Act
        boolean isHighRisk = blowoutRiskService.isHighBlowoutRisk(game);

        // Assert
        assertTrue(isHighRisk, "Should return true for significant team strength mismatch");
    }

    // Helper methods
    private List<AdvancedGameStats> createAdvancedGameStats(double netRating, double pace) {
        AdvancedGameStats stats = new AdvancedGameStats();
        stats.setNetRating(BigDecimal.valueOf(netRating));
        stats.setPace(BigDecimal.valueOf(pace));
        return List.of(stats);
    }

    private List<Games> createBlowoutGames() {
        Games game1 = createGame(120, 95);
        Games game2 = createGame(115, 90);
        return Arrays.asList(game1, game2);
    }

    private List<Games> createCompetitiveGames() {
        Games game1 = createGame(105, 100);
        Games game2 = createGame(98, 95);
        return Arrays.asList(game1, game2);
    }

    private Games createGame(int homeScore, int awayScore) {
        Games game = new Games();
        game.setHomeTeamScore(homeScore);
        game.setAwayTeamScore(awayScore);
        return game;
    }
}