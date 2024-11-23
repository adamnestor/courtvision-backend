package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
public class BaseTestSetup {

    @Autowired
    protected TeamsRepository teamsRepository;
    @Autowired
    protected PlayersRepository playersRepository;
    @Autowired
    protected GamesRepository gamesRepository;
    @Autowired
    protected GameStatsRepository gameStatsRepository;
    @Autowired
    protected HitRatesRepository hitRatesRepository;
    @Autowired
    protected UserPicksRepository userPicksRepository;    // Add for cleanup only
    @Autowired
    protected UsersRepository usersRepository;

    protected Teams testTeam;
    protected Players testPlayer;
    protected Games testGame;
    protected GameStats testGameStats;
    protected HitRates testHitRates;

    @BeforeEach
    void setup() {
        // Clean up existing data
        userPicksRepository.deleteAll();
        hitRatesRepository.deleteAll();
        gameStatsRepository.deleteAll();
        gamesRepository.deleteAll();
        playersRepository.deleteAll();
        teamsRepository.deleteAll();
        usersRepository.deleteAll();

        // Create and save test team
        testTeam = new Teams();
        testTeam.setName("Test Team");
        testTeam.setCity("Test City");
        testTeam.setAbbreviation("TST");
        testTeam.setConference(Conference.East);
        testTeam.setDivision("Test Division");
        testTeam.setExternalId(1L);
        testTeam = teamsRepository.save(testTeam);

        // Create and save test player
        testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setTeam(testTeam);
        testPlayer.setExternalId(1L);
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer = playersRepository.save(testPlayer);

        // Create and save test game
        testGame = new Games();
        testGame.setGameDate(LocalDate.now());
        testGame.setHomeTeam(testTeam);
        testGame.setAwayTeam(testTeam);
        testGame.setStatus(GameStatus.FINAL);
        testGame.setSeason(2024);
        testGame.setExternalId(1L);
        testGame = gamesRepository.save(testGame);

        // Create and save test game stats
        testGameStats = new GameStats();
        testGameStats.setPlayer(testPlayer);
        testGameStats.setGame(testGame);
        testGameStats.setPoints(20);
        testGameStats.setAssists(5);
        testGameStats.setRebounds(5);
        testGameStats = gameStatsRepository.save(testGameStats);

        // Create and save test hit rates
        testHitRates = new HitRates();
        testHitRates.setPlayer(testPlayer);
        testHitRates.setCategory(StatCategory.POINTS);
        testHitRates.setTimePeriod(TimePeriod.L10);
        testHitRates.setThreshold(20);
        testHitRates.setHitRate(new BigDecimal("80.0"));
        testHitRates.setAverage(new BigDecimal("22.5"));
        testHitRates.setGamesCounted(10);
        testHitRates.setLastCalculated(LocalDateTime.now());
        testHitRates = hitRatesRepository.save(testHitRates);
    }
}