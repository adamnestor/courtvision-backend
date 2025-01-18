package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import com.adamnestor.courtvision.service.PickResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PickResultIntegrationTest {

    @Autowired
    private PickResultService pickResultService;

    @Autowired
    private UserPicksRepository userPicksRepository;

    @Autowired
    private GameStatsRepository gameStatsRepository;

    @Autowired
    private PlayersRepository playersRepository;

    @Autowired
    private GamesRepository gamesRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    @Autowired
    private UsersRepository usersRepository;

    private Players testPlayer;
    private Games testGame;
    private Users testUser;
    private Teams homeTeam;
    private Teams awayTeam;

    @BeforeEach
    void setUp() {
        // Create test data
        testPlayer = createTestPlayer();
        testGame = createTestGame();
        testUser = createTestUser();
        createTestPicks();
    }

    @Test
    void testIndividualPickProcessing() {
        UserPicks pick = createUserPick(StatCategory.POINTS, 20);
        createGameStats(25); // Player scored 25 points

        pickResultService.processPickResult(pick);

        UserPicks processedPick = userPicksRepository.findById(pick.getId()).orElseThrow();
        assertTrue(processedPick.getResult());
    }

    @Test
    void testParlayProcessing() {
        String parlayId = "TEST-PARLAY-001";
        UserPicks pick1 = createUserPick(StatCategory.POINTS, 20);
        UserPicks pick2 = createUserPick(StatCategory.ASSISTS, 5);
        pick1.setParlayId(parlayId);
        pick2.setParlayId(parlayId);

        createGameStats(25, 6); // Player got 25 points and 6 assists

        pickResultService.processPickResult(pick1);
        pickResultService.processPickResult(pick2);

        List<UserPicks> processedPicks = userPicksRepository.findByParlayId(parlayId);
        assertTrue(processedPicks.stream().allMatch(pick -> Boolean.TRUE.equals(pick.getResult())));
    }

    @Test
    void testMissingGameStatsHandling() {
        UserPicks pick = createUserPick(StatCategory.POINTS, 20);
        // Don't create game stats to test missing data scenario

        pickResultService.processPickResult(pick);

        UserPicks processedPick = userPicksRepository.findById(pick.getId()).orElseThrow();
        assertNull(processedPick.getResult());
    }

    private Players createTestPlayer() {
        Players player = new Players();
        player.setFirstName("Test");
        player.setLastName("Player");
        player.setStatus(PlayerStatus.ACTIVE);
        player.setExternalId(999999L);
        return playersRepository.save(player);
    }

    private Games createTestGame() {
        // Create test teams if not already created
        if (homeTeam == null) {
            homeTeam = new Teams();
            homeTeam.setName("Home Team");
            homeTeam.setExternalId(777777L);
            homeTeam.setAbbreviation("HT");
            homeTeam.setCity("Home City");
            homeTeam.setConference(Conference.East);
            homeTeam.setDivision("Atlantic");
            homeTeam = teamsRepository.save(homeTeam);
        }

        if (awayTeam == null) {
            awayTeam = new Teams();
            awayTeam.setName("Away Team");
            awayTeam.setExternalId(666666L);
            awayTeam.setAbbreviation("AT");
            awayTeam.setCity("Away City");
            awayTeam.setConference(Conference.West);
            awayTeam.setDivision("Pacific");
            awayTeam = teamsRepository.save(awayTeam);
        }

        Games game = new Games();
        game.setGameDate(LocalDate.now().minusDays(1));
        game.setGameTime("7:00 PM ET");
        game.setStatus("FINAL");
        game.setExternalId(888888L);
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setSeason(2024);
        return gamesRepository.save(game);
    }

    private Users createTestUser() {
        Users user = new Users();
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"); // hash for "password"
        return usersRepository.save(user);
    }

    private UserPicks createUserPick(StatCategory category, int threshold) {
        UserPicks pick = new UserPicks();
        pick.setUser(testUser);
        pick.setPlayer(testPlayer);
        pick.setGame(testGame);
        pick.setCategory(category);
        pick.setThreshold(threshold);
        pick.setResult(null);
        return userPicksRepository.save(pick);
    }

    private void createGameStats(int points) {
        createGameStats(points, 0);
    }

    private void createGameStats(int points, int assists) {
        GameStats stats = new GameStats();
        stats.setPlayer(testPlayer);
        stats.setGame(testGame);
        stats.setPoints(points);
        stats.setAssists(assists);
        gameStatsRepository.save(stats);
    }

    private void createTestPicks() {
        createUserPick(StatCategory.POINTS, 20);
        createUserPick(StatCategory.ASSISTS, 5);
    }
} 