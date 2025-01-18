package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.confidence.service.ConfidenceScoreService;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ConfidenceScoreIntegrationTest {

    @Autowired
    private ConfidenceScoreService confidenceScoreService;

    @Autowired
    private PlayersRepository playersRepository;

    @Autowired
    private GamesRepository gamesRepository;

    @Autowired
    private GameStatsRepository gameStatsRepository;

    private Players testPlayer;
    private Games testGame;

    @BeforeEach
    void setUp() {
        // Create test player
        testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer = playersRepository.save(testPlayer);

        // Create test game
        testGame = new Games();
        testGame.setGameDate(LocalDate.now().plusDays(1));
        testGame.setGameTime("7:00 PM ET");
        testGame.setStatus("SCHEDULED");
        testGame = gamesRepository.save(testGame);

        // Create historical game stats
        createHistoricalGameStats();
    }

    @Test
    void testConfidenceScoreCalculation() {
        BigDecimal score = confidenceScoreService.calculateConfidenceScore(
            testPlayer,
            testGame,
            20,
            StatCategory.POINTS
        );

        assertNotNull(score);
        assertTrue(score.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(score.compareTo(new BigDecimal("100")) <= 0);
    }

    @Test
    void testRestImpactIntegration() {
        // Create a back-to-back game scenario
        Games previousGame = new Games();
        previousGame.setGameDate(LocalDate.now().minusDays(1));
        previousGame.setGameTime("7:00 PM ET");
        previousGame.setStatus("SCHEDULED");
        gamesRepository.save(previousGame);

        BigDecimal score = confidenceScoreService.calculateConfidenceScore(
            testPlayer,
            testGame,
            20,
            StatCategory.POINTS
        );

        assertNotNull(score);
        assertTrue(score.compareTo(BigDecimal.ZERO) >= 0);
    }

    private void createHistoricalGameStats() {
        // Create 10 games worth of historical stats
        for (int i = 0; i < 10; i++) {
            GameStats stats = new GameStats();
            stats.setPlayer(testPlayer);
            stats.setPoints(20 + (i % 5));
            stats.setAssists(5 + (i % 3));
            stats.setRebounds(8 + (i % 4));
            
            Games game = new Games();
            game.setGameDate(LocalDate.now().minusDays(i + 1));
            game.setGameTime("7:00 PM ET");
            game.setStatus("FINAL");
            game = gamesRepository.save(game);
            
            stats.setGame(game);
            gameStatsRepository.save(stats);
        }
    }
} 