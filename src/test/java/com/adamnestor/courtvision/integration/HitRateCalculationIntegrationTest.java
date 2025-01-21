package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.repository.TeamsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.service.HitRateCalculationService;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    }

    @Test
    void testCalculateHitRate() {
        createTestGameStats(10);  // Create 10 games with known stats

        Map<String, Object> result = hitRateCalculationService.calculateHitRate(
            testPlayer,
            StatCategory.POINTS,
            20,  // threshold
            TimePeriod.L10
        );

        assertNotNull(result);
        assertTrue(result.containsKey("hitRate"));
        assertTrue(result.containsKey("average"));
        
        // Since we set all games to 25 points, hit rate should be 100%
        assertEquals(new BigDecimal("100.0000"), result.get("hitRate"));
        assertEquals(new BigDecimal("25.0000"), result.get("average"));
    }

    @Test
    void testCalculateHitRateWithMixedResults() {
        // Create 10 games with alternating hits/misses
        for (int i = 0; i < 10; i++) {
            createGameWithStats(i, i % 2 == 0 ? 25 : 15);  // Alternating above/below 20
        }

        Map<String, Object> result = hitRateCalculationService.calculateHitRate(
            testPlayer,
            StatCategory.POINTS,
            20,
            TimePeriod.L10
        );

        // Should have 50% hit rate (5 games above threshold)
        assertEquals(new BigDecimal("50.0000"), result.get("hitRate"));
        assertEquals(new BigDecimal("20.0000"), result.get("average"));
    }

    @Test
    void testInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> 
            hitRateCalculationService.calculateHitRate(null, StatCategory.POINTS, 20, TimePeriod.L10));
        
        assertThrows(IllegalArgumentException.class, () -> 
            hitRateCalculationService.calculateHitRate(testPlayer, null, 20, TimePeriod.L10));
        
        assertThrows(IllegalArgumentException.class, () -> 
            hitRateCalculationService.calculateHitRate(testPlayer, StatCategory.POINTS, 52, TimePeriod.L10));
    }

    @Test
    void testGetPlayerAverages() {
        createTestGameStats(10);

        Map<StatCategory, BigDecimal> averages = hitRateCalculationService.getPlayerAverages(
            testPlayer,
            TimePeriod.L10
        );

        assertNotNull(averages);
        assertTrue(averages.containsKey(StatCategory.POINTS));
        assertTrue(averages.containsKey(StatCategory.ASSISTS));
        assertTrue(averages.containsKey(StatCategory.REBOUNDS));
    }

    @Test
    void testHasSufficientData() {
        assertFalse(hitRateCalculationService.hasSufficientData(testPlayer, TimePeriod.L10));
        
        createTestGameStats(10);
        
        assertTrue(hitRateCalculationService.hasSufficientData(testPlayer, TimePeriod.L10));
    }

    @Test
    void testPlayerDetailStats() {
        PlayerDetailStats stats = hitRateCalculationService.getPlayerDetailStats(
            testPlayer.getId(), TimePeriod.L10, StatCategory.POINTS, 20);

        assertNotNull(stats);
        assertEquals(testPlayer.getId(), stats.playerId());
        assertEquals(testPlayer.getFirstName() + " " + testPlayer.getLastName(), stats.playerName());
        assertEquals(testPlayer.getTeam().getAbbreviation(), stats.team());
        assertNotNull(stats.hitRate());
        assertNotNull(stats.confidenceScore());
        assertNotNull(stats.gamesPlayed());
        assertNotNull(stats.average());
    }

    private void createTestGameStats(int numberOfGames) {
        for (int i = 0; i < numberOfGames; i++) {
            Games game = new Games();
            game.setGameDate(LocalDate.now().minusDays(i));
            game.setGameTime("7:00 PM ET");
            game.setStatus("FINAL");
            game.setExternalId(100000L + i);
            game.setHomeTeam(homeTeam);
            game.setAwayTeam(awayTeam);
            game.setSeason(2024);
            game = gamesRepository.save(game);

            GameStats stats = new GameStats();
            stats.setPlayer(testPlayer);
            stats.setGame(game);
            // Set predictable stats for testing
            stats.setPoints(25);  // Always above 20 threshold
            stats.setAssists(6);  // Always above 5 threshold
            stats.setRebounds(10); // Always above 8 threshold
            gameStatsRepository.save(stats);
        }
    }

    private void createGameWithStats(int daysAgo, int points) {
        Games game = new Games();
        game.setGameDate(LocalDate.now().minusDays(daysAgo));
        game.setGameTime("7:00 PM ET");
        game.setStatus("FINAL");
        game.setExternalId(100000L + daysAgo);
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setSeason(2024);
        game = gamesRepository.save(game);

        GameStats stats = new GameStats();
        stats.setPlayer(testPlayer);
        stats.setGame(game);
        stats.setPoints(points);
        stats.setAssists(5);
        stats.setRebounds(5);
        gameStatsRepository.save(stats);
    }
} 