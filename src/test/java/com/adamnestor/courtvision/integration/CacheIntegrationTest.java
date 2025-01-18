package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.service.CacheIntegrationService;
import com.adamnestor.courtvision.service.DailyRefreshService;
import com.adamnestor.courtvision.service.WarmingStrategyService;
import com.adamnestor.courtvision.config.CacheConfig;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.GameStatus;
import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.TeamsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import com.adamnestor.courtvision.config.TestCacheConfig;
import com.adamnestor.courtvision.domain.Conference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestCacheConfig.class)
public class CacheIntegrationTest {

    @Autowired
    private CacheIntegrationService cacheIntegrationService;

    @Autowired
    private DailyRefreshService dailyRefreshService;

    @Autowired
    private WarmingStrategyService warmingStrategyService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PlayersRepository playersRepository;

    @Autowired
    private GameStatsRepository gameStatsRepository;

    @Autowired
    private GamesRepository gamesRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    private Players testPlayer;

    @BeforeEach
    void setUp() {
        // Clean Redis and database before each test
        var factory = redisTemplate.getConnectionFactory();
        if (factory != null) {
            factory.getConnection().serverCommands().flushAll();
        }
        
        // Clean up database tables in correct order
        gameStatsRepository.deleteAll();  // Delete child records first
        gamesRepository.deleteAll();      // Delete games before teams
        playersRepository.deleteAll();    // Delete players
        teamsRepository.deleteAll();      // Delete teams last
        
        // Create test player with unique ID
        testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer.setExternalId(System.currentTimeMillis());  // Use timestamp for unique ID
        testPlayer.setCreatedAt(LocalDateTime.now());
        testPlayer.setUpdatedAt(LocalDateTime.now());
        testPlayer = playersRepository.save(testPlayer);

        createTestGameStats();
    }

    private void createTestGameStats() {
        // Create test teams first
        Teams homeTeam = new Teams();
        homeTeam.setName("Home Team");
        homeTeam.setAbbreviation("HT");
        homeTeam.setCity("Home City");
        homeTeam.setConference(Conference.East);
        homeTeam.setDivision("Atlantic");
        homeTeam.setExternalId(System.currentTimeMillis() + 2);
        homeTeam.setCreatedAt(LocalDateTime.now());
        homeTeam.setUpdatedAt(LocalDateTime.now());
        homeTeam = teamsRepository.save(homeTeam);

        Teams awayTeam = new Teams();
        awayTeam.setName("Away Team");
        awayTeam.setAbbreviation("AT");
        awayTeam.setCity("Away City");
        awayTeam.setConference(Conference.West);
        awayTeam.setDivision("Pacific");
        awayTeam.setExternalId(System.currentTimeMillis() + 3);
        awayTeam.setCreatedAt(LocalDateTime.now());
        awayTeam.setUpdatedAt(LocalDateTime.now());
        awayTeam = teamsRepository.save(awayTeam);

        // Create game with teams
        Games game = new Games();
        game.setGameDate(LocalDate.now().atStartOfDay());
        game.setStatus(GameStatus.SCHEDULED);
        game.setExternalId(System.currentTimeMillis() + 1);
        game.setCreatedAt(LocalDateTime.now());
        game.setUpdatedAt(LocalDateTime.now());
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setSeason(2024);
        
        game = gamesRepository.save(game);
        
        GameStats stats = new GameStats();
        stats.setPlayer(testPlayer);
        stats.setGame(game);
        stats.setPoints(20);
        stats.setAssists(5);
        stats.setRebounds(8);
        
        gameStatsRepository.save(stats);
    }

    @Test
    void testDailyRefreshProcess() {
        // Execute daily refresh
        dailyRefreshService.performDailyRefresh();

        // Verify cache state
        assertTrue(cacheIntegrationService.verifyDataSynchronization());
    }

    @Test
    void testCacheWarmingStrategy() {
        // Test priority warming
        warmingStrategyService.executeWarmingStrategy(
            WarmingStrategyService.WarmingPriority.HIGH
        );

        // Verify cache contains expected data
        String keyPattern = CacheConfig.PLAYER_KEY_PREFIX + ":" + CacheConfig.STATS_KEY_PREFIX + ":*";
        assertTrue(redisTemplate.hasKey(keyPattern));
    }

    @Test
    void testErrorRecoveryMechanism() {
        // Simulate failure and test recovery
        cacheIntegrationService.handleUpdateFailure("daily-update");

        // Verify system recovered properly
        assertTrue(cacheIntegrationService.verifyDataSynchronization());
    }
} 