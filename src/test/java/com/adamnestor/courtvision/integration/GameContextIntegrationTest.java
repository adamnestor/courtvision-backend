package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.confidence.service.GameContextService;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import com.adamnestor.courtvision.confidence.model.GameContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GameContextIntegrationTest {

    @Autowired
    private GameContextService gameContextService;

    @Autowired
    private GameStatsRepository gameStatsRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    @Autowired
    private PlayersRepository playersRepository;

    private Players testPlayer;
    private Teams homeTeam;
    private Teams awayTeam;
    private Games testGame;

    @BeforeEach
    void setUp() {
        // Create teams
        homeTeam = new Teams();
        homeTeam.setName("Home Team");
        homeTeam.setAbbreviation("HOME");
        homeTeam = teamsRepository.save(homeTeam);

        awayTeam = new Teams();
        awayTeam.setName("Away Team");
        awayTeam.setAbbreviation("AWAY");
        awayTeam = teamsRepository.save(awayTeam);

        // Create player
        testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setTeam(homeTeam);
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer = playersRepository.save(testPlayer);

        // Create game
        testGame = new Games();
        testGame.setHomeTeam(homeTeam);
        testGame.setAwayTeam(awayTeam);
        testGame.setGameDate(LocalDateTime.now().plusDays(1));
        testGame.setStatus(GameStatus.SCHEDULED);

        createHistoricalMatchupData();
    }

    @Test
    void testMatchupImpactCalculation() {
        GameContext context = gameContextService.calculateGameContext(
            testPlayer,
            testGame,
            StatCategory.POINTS,
            20
        );

        assertNotNull(context);
        assertNotNull(context.getMatchupImpact());
        assertTrue(context.getMatchupImpact().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testDefensiveImpactAnalysis() {
        GameContext context = gameContextService.calculateGameContext(
            testPlayer,
            testGame,
            StatCategory.POINTS,
            20
        );

        assertNotNull(context);
        assertNotNull(context.getDefensiveImpact());
        assertTrue(context.getDefensiveImpact().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testVenueImpactAssessment() {
        GameContext context = gameContextService.calculateGameContext(
            testPlayer,
            testGame,
            StatCategory.POINTS,
            20
        );

        assertNotNull(context);
        assertNotNull(context.getVenueImpact());
        assertTrue(context.getVenueImpact().compareTo(BigDecimal.ZERO) > 0);
    }

    private void createHistoricalMatchupData() {
        // Create historical game stats against the opponent
        for (int i = 0; i < 5; i++) {
            Games historicalGame = new Games();
            historicalGame.setHomeTeam(homeTeam);
            historicalGame.setAwayTeam(awayTeam);
            historicalGame.setGameDate(LocalDateTime.now().minusDays(i + 1));
            historicalGame.setStatus(GameStatus.FINAL);

            GameStats stats = new GameStats();
            stats.setGame(historicalGame);
            stats.setPlayer(testPlayer);
            stats.setPoints(20 + (i % 5));
            
            gameStatsRepository.save(stats);
        }
    }
} 