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

import java.time.LocalDate;
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

    @Autowired
    private GamesRepository gamesRepository;

    private Players testPlayer;
    private Teams homeTeam;
    private Teams awayTeam;
    private Games testGame;

    @BeforeEach
    void setUp() {
        // Create teams with complete data
        homeTeam = new Teams();
        homeTeam.setName("Home Team");
        homeTeam.setExternalId(777777L);
        homeTeam.setAbbreviation("HT");
        homeTeam.setCity("Home City");
        homeTeam.setConference(Conference.East);
        homeTeam.setDivision("Atlantic");
        homeTeam = teamsRepository.save(homeTeam);

        awayTeam = new Teams();
        awayTeam.setName("Away Team");
        awayTeam.setExternalId(666666L);
        awayTeam.setAbbreviation("AT");
        awayTeam.setCity("Away City");
        awayTeam.setConference(Conference.West);
        awayTeam.setDivision("Pacific");
        awayTeam = teamsRepository.save(awayTeam);

        // Create player with complete data
        testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setTeam(homeTeam);
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer.setExternalId(999999L);
        testPlayer = playersRepository.save(testPlayer);

        // Create game with complete data
        testGame = new Games();
        testGame.setHomeTeam(homeTeam);
        testGame.setAwayTeam(awayTeam);
        testGame.setGameDate(LocalDate.now().plusDays(1));
        testGame.setGameTime("7:00 PM ET");
        testGame.setStatus("SCHEDULED");
        testGame.setExternalId(888888L);
        testGame.setSeason(2024);

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
            historicalGame.setGameDate(LocalDate.now().minusDays(i + 1));
            historicalGame.setGameTime("7:00 PM ET");
            historicalGame.setStatus("FINAL");
            historicalGame.setExternalId(100000L + i);  // Consistent test IDs
            historicalGame.setSeason(2024);
            historicalGame = gamesRepository.save(historicalGame);

            GameStats stats = new GameStats();
            stats.setGame(historicalGame);
            stats.setPlayer(testPlayer);
            stats.setPoints(20 + (i % 5));
            gameStatsRepository.save(stats);
        }
    }
} 