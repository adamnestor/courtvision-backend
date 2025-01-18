package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.repository.TeamsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.service.HitRateCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnableAutoConfiguration(exclude = {CacheAutoConfiguration.class})
public class HitRateCalculationIntegrationTest {

    @Autowired
    private HitRateCalculationService hitRateCalculationService;

    @Autowired
    private GameStatsRepository gameStatsRepository;

    @Autowired
    private PlayersRepository playersRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    @Autowired
    private GamesRepository gamesRepository;

    private Players testPlayer;
    private Teams homeTeam;
    private Teams awayTeam;

    @BeforeEach
    void setUp() {
        // Create test teams
        homeTeam = createTestTeam("Home", "HT", Conference.East);
        awayTeam = createTestTeam("Away", "AT", Conference.West);

        // Create test player
        testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer.setExternalId(999999L);
        testPlayer.setTeam(homeTeam);  // Set player's team
        testPlayer = playersRepository.save(testPlayer);

        // Create test game stats
        createTestGameStats();
    }

    private Teams createTestTeam(String name, String abbrev, Conference conf) {
        Teams team = new Teams();
        team.setName(name + " Team");
        team.setExternalId(System.nanoTime());  // Unique ID
        team.setAbbreviation(abbrev);
        team.setCity(name + " City");
        team.setConference(conf);
        team.setDivision(conf == Conference.East ? "Atlantic" : "Pacific");
        return teamsRepository.save(team);
    }

    @Test
    void testHitRateCalculationAcrossTimePeriods() {
        // Test for different periods (L5, L10, L15, L20)
        TimePeriod[] periods = {TimePeriod.L5, TimePeriod.L10, TimePeriod.L15, TimePeriod.L20};
        
        for (TimePeriod period : periods) {
            Map<String, Object> result = hitRateCalculationService.calculateHitRate(
                testPlayer,
                StatCategory.POINTS,
                20,
                period
            );
            
            assertNotNull(result);
            assertTrue(result.containsKey("hitRate"));
            assertTrue(result.containsKey("gamesPlayed"));
        }
    }

    @Test
    void testThresholdCalculations() {
        // Test points thresholds
        int[] pointsThresholds = {10, 15, 20, 25};
        for (int threshold : pointsThresholds) {
            Map<String, Object> result = hitRateCalculationService.calculateHitRate(
                testPlayer,
                StatCategory.POINTS,
                threshold,
                TimePeriod.L10
            );
            assertNotNull(result);
        }

        // Test assists thresholds
        int[] assistsThresholds = {2, 4, 6, 8};
        for (int threshold : assistsThresholds) {
            Map<String, Object> result = hitRateCalculationService.calculateHitRate(
                testPlayer,
                StatCategory.ASSISTS,
                threshold,
                TimePeriod.L10
            );
            assertNotNull(result);
        }
    }

    @Test
    void testInsufficientDataHandling() {
        // Clear existing game stats
        gameStatsRepository.deleteAll();

        Map<String, Object> result = hitRateCalculationService.calculateHitRate(
            testPlayer,
            StatCategory.POINTS,
            20,
            TimePeriod.L10
        );

        assertNotNull(result);
        assertEquals(0, ((BigDecimal) result.get("hitRate")).intValue());
        assertEquals(0, result.get("gamesPlayed"));
    }

    private void createTestGameStats() {
        // Create 20 games worth of stats
        for (int i = 0; i < 20; i++) {
            Games game = new Games();
            game.setGameDate(LocalDateTime.now().minusDays(i));
            game.setStatus(GameStatus.FINAL);
            game.setExternalId(100000L + i);
            game.setHomeTeam(homeTeam);
            game.setAwayTeam(awayTeam);
            game.setSeason(2024);
            game = gamesRepository.save(game);

            GameStats stats = new GameStats();
            stats.setPlayer(testPlayer);
            stats.setPoints(20 + (i % 10));
            stats.setAssists(5 + (i % 5));
            stats.setRebounds(8 + (i % 4));
            stats.setGame(game);
            
            gameStatsRepository.save(stats);
        }
    }
} 