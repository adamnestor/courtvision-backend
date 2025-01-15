package com.adamnestor.courtvision.test.confidence.service.impl;

import com.adamnestor.courtvision.confidence.model.GameContext;
import com.adamnestor.courtvision.confidence.model.RestImpact;
import com.adamnestor.courtvision.confidence.service.*;
import com.adamnestor.courtvision.confidence.service.impl.ConfidenceScoreServiceImpl;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ConfidenceScoreServiceImplTest {

    @Mock
    private GameStatsRepository gameStatsRepository;
    @Mock
    private AdvancedGameStatsRepository advancedStatsRepository;
    @Mock
    private PlayersRepository playersRepository;
    @Mock
    private RestImpactService restImpactService;
    @Mock
    private GameContextService gameContextService;
    @Mock
    private AdvancedMetricsService advancedMetricsService;
    @Mock
    private BlowoutRiskService blowoutRiskService;

    private ConfidenceScoreServiceImpl confidenceScoreService;

    private Players testPlayer;
    private Games testGame;
    private Teams homeTeam;
    private Teams awayTeam;

    @BeforeEach
    void setUp() {
        confidenceScoreService = new ConfidenceScoreServiceImpl(
                gameStatsRepository,
                advancedStatsRepository,
                playersRepository,
                restImpactService,
                gameContextService,
                advancedMetricsService,
                blowoutRiskService
        );

        homeTeam = new Teams();
        homeTeam.setId(1L);
        homeTeam.setName("Home Team");

        awayTeam = new Teams();
        awayTeam.setId(2L);
        awayTeam.setName("Away Team");

        testPlayer = new Players();
        testPlayer.setId(1L);
        testPlayer.setTeam(homeTeam);

        testGame = new Games();
        testGame.setId(1L);
        testGame.setHomeTeam(homeTeam);
        testGame.setAwayTeam(awayTeam);
        testGame.setGameDate(LocalDateTime.now());
    }

    @Test
    void calculateConfidenceScore_NormalCase() {
        // Arrange
        GameContext gameContext = new GameContext(
                new BigDecimal("75.00"),
                new BigDecimal("80.00"),
                new BigDecimal("85.00"),
                new BigDecimal("80.00"),
                StatCategory.POINTS
        );

        RestImpact restImpact = new RestImpact(
                1,
                BigDecimal.ONE,
                BigDecimal.ONE,
                LocalDate.now()
        );

        // Mock only the methods that are actually called
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(createMockGameStats());
        when(advancedMetricsService.calculateAdvancedImpact(testPlayer, testGame, StatCategory.POINTS))
                .thenReturn(new BigDecimal("75.00"));
        when(gameContextService.calculateGameContext(testPlayer, testGame, StatCategory.POINTS, 20))
                .thenReturn(gameContext);
        when(restImpactService.calculateRestImpact(testPlayer, testGame, StatCategory.POINTS))
                .thenReturn(restImpact);
        when(playersRepository.findByTeamIdInAndStatus(any(), any()))
                .thenReturn(Arrays.asList(testPlayer));
        when(blowoutRiskService.calculateBlowoutRisk(testGame))
                .thenReturn(new BigDecimal("40.00"));

        // Act
        BigDecimal result = confidenceScoreService.calculateConfidenceScore(
                testPlayer,
                testGame,
                20,
                StatCategory.POINTS
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.compareTo(new BigDecimal("100")) <= 0);
    }

    @Test
    void calculateRecentPerformance_WithEmptyGamesList() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(Collections.emptyList());

        // Act
        BigDecimal result = confidenceScoreService.calculateRecentPerformance(
                testPlayer,
                20,
                StatCategory.POINTS
        );

        // Assert
        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateRecentPerformance_WithGamesList() {
        // Arrange
        List<GameStats> mockStats = createMockGameStats();
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(mockStats);

        // Act
        BigDecimal result = confidenceScoreService.calculateRecentPerformance(
                testPlayer,
                20,
                StatCategory.POINTS
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void calculateBlowoutRisk_WithValidGame() {
        // Arrange
        when(playersRepository.findByTeamIdInAndStatus(any(), any())).thenReturn(Arrays.asList(testPlayer));
        when(blowoutRiskService.calculateBlowoutRisk(any())).thenReturn(new BigDecimal("45.00"));

        // Act
        BigDecimal result = confidenceScoreService.calculateBlowoutRisk(testGame);

        // Assert
        assertEquals(new BigDecimal("45.00"), result);
    }

    @Test
    void calculateConfidenceScore_WithHighBlowoutRisk() {
        // Arrange
        GameContext gameContext = new GameContext(
                new BigDecimal("75.00"),
                new BigDecimal("80.00"),
                new BigDecimal("85.00"),
                new BigDecimal("80.00"),
                StatCategory.POINTS
        );

        RestImpact restImpact = new RestImpact(
                1,
                BigDecimal.ONE,
                BigDecimal.ONE,
                LocalDate.now()
        );

        // Mock with specific parameters
        when(gameStatsRepository.findPlayerRecentGames(testPlayer))
                .thenReturn(createMockGameStats());
        when(advancedMetricsService.calculateAdvancedImpact(testPlayer, testGame, StatCategory.POINTS))
                .thenReturn(new BigDecimal("75.00"));
        when(gameContextService.calculateGameContext(testPlayer, testGame, StatCategory.POINTS, 20))
                .thenReturn(gameContext);
        when(restImpactService.calculateRestImpact(testPlayer, testGame, StatCategory.POINTS))
                .thenReturn(restImpact);
        when(playersRepository.findByTeamIdInAndStatus(any(), any()))
                .thenReturn(Arrays.asList(testPlayer));
        when(blowoutRiskService.calculateBlowoutRisk(testGame))
                .thenReturn(new BigDecimal("70.00")); // High blowout risk

        // Act
        BigDecimal result = confidenceScoreService.calculateConfidenceScore(
                testPlayer,
                testGame,
                20,
                StatCategory.POINTS
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.compareTo(new BigDecimal("100")) <= 0);
    }

    @Test
    void calculateGameContext() {
        // Arrange
        GameContext mockContext = new GameContext(
                new BigDecimal("75.00"),
                new BigDecimal("80.00"),
                new BigDecimal("85.00"),
                new BigDecimal("80.00"),
                StatCategory.POINTS
        );

        when(gameContextService.calculateGameContext(any(), any(), any(), any())).thenReturn(mockContext);

        // Act
        BigDecimal result = confidenceScoreService.calculateGameContext(testPlayer, testGame, StatCategory.POINTS);

        // Assert
        assertEquals(mockContext.getOverallScore().setScale(2), result);
    }

    // Helper method to create mock game stats
    private List<GameStats> createMockGameStats() {
        GameStats stats1 = new GameStats();
        stats1.setPoints(25);
        stats1.setAssists(5);
        stats1.setRebounds(8);
        stats1.setGame(testGame);

        GameStats stats2 = new GameStats();
        stats2.setPoints(22);
        stats2.setAssists(6);
        stats2.setRebounds(7);
        stats2.setGame(testGame);

        return Arrays.asList(stats1, stats2);
    }
}