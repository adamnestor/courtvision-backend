package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.confidence.service.AdvancedMetricsService;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdvancedMetricsIntegrationTest {

    @Autowired
    private AdvancedMetricsService advancedMetricsService;

    @Autowired
    private AdvancedGameStatsRepository advancedStatsRepository;

    @Autowired
    private PlayersRepository playersRepository;

    @Autowired
    private GamesRepository gamesRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    private Players testPlayer;
    private Games testGame;
    private Teams homeTeam;
    private Teams awayTeam;

    @BeforeEach
    void setUp() {
        // Create test teams
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

        // Create test player
        testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer.setExternalId(999999L);
        testPlayer.setTeam(homeTeam);
        testPlayer = playersRepository.save(testPlayer);

        // Create test game
        testGame = new Games();
        testGame.setGameDate(LocalDateTime.now().plusDays(1));
        testGame.setStatus(GameStatus.SCHEDULED);
        testGame.setExternalId(888888L);
        testGame.setHomeTeam(homeTeam);
        testGame.setAwayTeam(awayTeam);
        testGame.setSeason(2024);
        testGame = gamesRepository.save(testGame);

        createAdvancedStatsHistory();
    }

    @Test
    void testPIECalculation() {
        BigDecimal impact = advancedMetricsService.analyzePIEImpact(
            testPlayer,
            20, // example threshold
            StatCategory.POINTS
        );

        assertNotNull(impact);
        assertTrue(impact.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(impact.compareTo(new BigDecimal("100")) <= 0);
    }

    @Test
    void testUsageRateAnalysis() {
        BigDecimal impact = advancedMetricsService.analyzeUsageRateImpact(
            testPlayer,
            testGame,
            StatCategory.POINTS
        );

        assertNotNull(impact);
        assertTrue(impact.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(impact.compareTo(new BigDecimal("100")) <= 0);
    }

    @Test
    void testEfficiencyMetrics() {
        BigDecimal impact = advancedMetricsService.calculateAdvancedImpact(
            testPlayer,
            testGame,
            StatCategory.POINTS
        );

        assertNotNull(impact);
        assertTrue(impact.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(impact.compareTo(new BigDecimal("100")) <= 0);
    }

    private void createAdvancedStatsHistory() {
        for (int i = 0; i < 10; i++) {
            Games game = new Games();
            game.setGameDate(LocalDateTime.now().minusDays(i + 1));
            game.setStatus(GameStatus.FINAL);
            game.setExternalId(100000L + i);
            game.setHomeTeam(homeTeam);
            game.setAwayTeam(awayTeam);
            game.setSeason(2024);
            game = gamesRepository.save(game);

            AdvancedGameStats stats = new AdvancedGameStats();
            stats.setPlayer(testPlayer);
            stats.setGame(game);
            stats.setPie(new BigDecimal("0.155").add(new BigDecimal(i * 0.01)));
            stats.setTrueShootingPercentage(new BigDecimal("55.5").add(new BigDecimal(i)));
            
            advancedStatsRepository.save(stats);
        }
    }
} 