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

    @Autowired
    private TeamsRepository teamsRepository;

    private Players testPlayer;
    private Games testGame;
    private Teams homeTeam;
    private Teams awayTeam;

    @BeforeEach
    void setUp() {
        // Create test teams with complete data
        homeTeam = new Teams();
        homeTeam.setName("Home Team");
        homeTeam.setExternalId(777777L);  // Using consistent test IDs
        homeTeam.setAbbreviation("HT");
        homeTeam.setCity("Home City");     // Added
        homeTeam.setConference(Conference.East);  // Added
        homeTeam.setDivision("Atlantic");  // Added
        homeTeam = teamsRepository.save(homeTeam);

        awayTeam = new Teams();
        awayTeam.setName("Away Team");
        awayTeam.setExternalId(666666L);   // Using consistent test IDs
        awayTeam.setAbbreviation("AT");
        awayTeam.setCity("Away City");      // Added
        awayTeam.setConference(Conference.West);  // Added
        awayTeam.setDivision("Pacific");    // Added
        awayTeam = teamsRepository.save(awayTeam);

        // Create test player with consistent ID
        testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer.setExternalId(999999L);  // Using consistent test ID
        testPlayer.setTeam(homeTeam);
        testPlayer = playersRepository.save(testPlayer);

        // Create test game with complete data
        testGame = new Games();
        testGame.setGameDate(LocalDate.now().plusDays(1));
        testGame.setGameTime("7:00 PM ET");
        testGame.setStatus("SCHEDULED");
        testGame.setExternalId(888888L);    // Using consistent test ID
        testGame.setHomeTeam(homeTeam);
        testGame.setAwayTeam(awayTeam);
        testGame.setSeason(2024);           // Added
        testGame = gamesRepository.save(testGame);

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
        Games previousGame = new Games();
        previousGame.setGameDate(LocalDate.now().minusDays(1));
        previousGame.setGameTime("7:00 PM ET");
        previousGame.setStatus("SCHEDULED");
        previousGame.setExternalId(4001L);    // Add consistent ID
        previousGame.setHomeTeam(homeTeam);   // Add teams
        previousGame.setAwayTeam(awayTeam);
        previousGame.setSeason(2024);         // Add season
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
            Games game = new Games();
            game.setGameDate(LocalDate.now().minusDays(i + 1));
            game.setGameTime("7:00 PM ET");
            game.setStatus("FINAL");
            game.setExternalId(3001L + i);
            game.setHomeTeam(homeTeam);
            game.setAwayTeam(awayTeam);
            game.setSeason(2024);
            game = gamesRepository.save(game);
            
            GameStats stats = new GameStats();
            stats.setPlayer(testPlayer);
            stats.setGame(game);
            stats.setPoints(20 + (i % 5));
            stats.setAssists(5 + (i % 3));
            stats.setRebounds(8 + (i % 4));
            gameStatsRepository.save(stats);
        }
    }
} 