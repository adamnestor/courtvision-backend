package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.hamcrest.Matchers;
import com.adamnestor.courtvision.confidence.service.ConfidenceScoreService;

import java.time.LocalDate;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class ResponseFormatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private PlayersRepository playersRepository;
    
    @Autowired
    private TeamsRepository teamsRepository;

    @Autowired
    private GamesRepository gamesRepository;

    @Autowired
    private UserPicksRepository userPicksRepository;

    @Autowired
    private GameStatsRepository gameStatsRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private HitRatesRepository hitRatesRepository;

    @Autowired
    private ConfidenceScoreService confidenceScoreService;

    private Players testPlayer;
    private Users user;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        gameStatsRepository.deleteAll();
        userPicksRepository.deleteAll();
        gamesRepository.deleteAll();
        playersRepository.deleteAll();
        teamsRepository.deleteAll();
        usersRepository.deleteAll();
        hitRatesRepository.deleteAll();

        // Create test user with matching email
        user = new Users();
        user.setEmail("user");
        user.setPasswordHash("password");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user = usersRepository.save(user);

        // Create test teams with complete data
        Teams homeTeam = new Teams();
        homeTeam.setName("Home Team");
        homeTeam.setExternalId(777777L);
        homeTeam.setAbbreviation("HT");
        homeTeam.setCity("Home City");
        homeTeam.setConference(Conference.East);
        homeTeam.setDivision("Atlantic");
        homeTeam = teamsRepository.save(homeTeam);

        Teams awayTeam = new Teams();
        awayTeam.setName("Away Team");
        awayTeam.setExternalId(666666L);
        awayTeam.setAbbreviation("AT");
        awayTeam.setCity("Away City");
        awayTeam.setConference(Conference.West);
        awayTeam.setDivision("Pacific");
        awayTeam = teamsRepository.save(awayTeam);

        // Create test player with complete data
        testPlayer = new Players();  // Store reference to player
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer.setExternalId(999999L);
        testPlayer.setTeam(homeTeam);
        testPlayer = playersRepository.save(testPlayer);

        // Create test game with complete data
        Games game = new Games();
        game.setGameDate(LocalDate.now().plusDays(1));
        game.setGameTime("7:00 PM ET");
        game.setStatus("SCHEDULED");
        game.setExternalId(888888L);
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setSeason(2024);
        gamesRepository.save(game);

        // Create historical game stats
        for (int i = 0; i < 5; i++) {
            Games pastGame = new Games();
            pastGame.setGameDate(LocalDate.now().minusDays(i + 1));
            pastGame.setGameTime("7:00 PM ET");
            pastGame.setStatus("FINAL");
            pastGame.setExternalId(444444L + i);
            pastGame.setHomeTeam(homeTeam);
            pastGame.setAwayTeam(awayTeam);
            pastGame.setSeason(2024);
            pastGame = gamesRepository.save(pastGame);

            GameStats stats = new GameStats();
            stats.setGame(pastGame);
            stats.setPlayer(testPlayer);
            stats.setPoints(25);  // Above threshold
            stats.setMinutesPlayed("30");
            gameStatsRepository.save(stats);
        }

        // Remove the hit rate loop since we only need one record
        HitRates hitRate = new HitRates();
        hitRate.setPlayer(testPlayer);
        hitRate.setCategory(StatCategory.POINTS);
        hitRate.setThreshold(20);
        hitRate.setTimePeriod(TimePeriod.L10);
        hitRate.setHitRate(BigDecimal.valueOf(80.0));
        hitRate.setGamesCounted(10);
        hitRate.setAverage(BigDecimal.valueOf(22.0));
        hitRate.setCreatedAt(LocalDate.now());
        hitRate.setLastCalculated(LocalDate.now());
        hitRatesRepository.save(hitRate);
    }

    @Test
    @WithMockUser
    void playerStatsResponse_ShouldMatchRequiredFormat() throws Exception {
        mockMvc.perform(get("/api/players/" + testPlayer.getId() + "/stats")  // Use actual ID
                .param("category", "POINTS")
                .param("threshold", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.playerId").exists())
                .andExpect(jsonPath("$.data.playerName").exists())
                .andExpect(jsonPath("$.data.team").exists())
                .andExpect(jsonPath("$.data.hitRate").isNumber())
                .andExpect(jsonPath("$.data.average").isNumber())
                .andExpect(jsonPath("$.data.confidenceScore").isNumber());
    }

    @Test
    @WithMockUser
    void picksResponse_ShouldMatchRequiredFormat() throws Exception {
        // Verify test data
        Users testUser = usersRepository.findByEmail("user")
            .orElseThrow(() -> new RuntimeException("Test user not found"));
        System.out.println("Test user picks: " + userPicksRepository.findByUser(testUser).size());

        MvcResult result = mockMvc.perform(get("/api/picks")
                .param("timePeriod", "L10")
                .param("category", "POINTS")
                .param("threshold", "20"))
                .andExpect(status().isOk())
                .andReturn();
                
        System.out.println("Response: " + result.getResponse().getContentAsString());

        // Then add our assertions
        mockMvc.perform(get("/api/picks")
                .param("timePeriod", "L10")
                .param("category", "POINTS")
                .param("threshold", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].playerId").exists())
                .andExpect(jsonPath("$.data[0].playerName").exists())
                .andExpect(jsonPath("$.data[0].team").exists())
                .andExpect(jsonPath("$.data[0].category").exists())
                .andExpect(jsonPath("$.data[0].threshold").exists())
                .andExpect(jsonPath("$.data[0].hitRateAtPick").isNumber())
                .andExpect(jsonPath("$.data[0].confidenceScore").isNumber())
                .andExpect(jsonPath("$.data[0].confidenceScore").value(Matchers.allOf(
                    Matchers.greaterThanOrEqualTo(0),
                    Matchers.lessThanOrEqualTo(100)
                )));
    }

    @Test
    @WithMockUser
    void dashboardResponse_ShouldMatchRequiredFormat() throws Exception {
        // First create some test data
        Teams homeTeam = new Teams();
        homeTeam.setName("Super Team");
        homeTeam.setExternalId(555555L);
        homeTeam.setAbbreviation("ST");
        homeTeam.setCity("Super City");
        homeTeam.setConference(Conference.East);
        homeTeam.setDivision("Atlantic");
        homeTeam = teamsRepository.save(homeTeam);

        Teams awayTeam = new Teams();
        awayTeam.setName("Away Team");
        awayTeam.setExternalId(555556L);
        awayTeam.setAbbreviation("ATT");
        awayTeam.setCity("Away City");
        awayTeam.setConference(Conference.West);
        awayTeam.setDivision("Pacific");
        awayTeam = teamsRepository.save(awayTeam);

        Players testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        testPlayer.setExternalId(666666L);
        testPlayer.setTeam(homeTeam);
        testPlayer.setCreatedAt(LocalDate.now());
        testPlayer = playersRepository.save(testPlayer);

        // Create a game for today
        Games todayGame = new Games();
        todayGame.setGameDate(LocalDate.now());
        todayGame.setHomeTeam(homeTeam);
        todayGame.setAwayTeam(awayTeam);
        todayGame.setStatus("SCHEDULED"); // Today's game should be scheduled
        todayGame.setExternalId(999999L);
        todayGame.setSeason(2024);
        todayGame.setGameTime("7:00 PM ET");
        todayGame = gamesRepository.save(todayGame);

        // Create historical game stats for confidence score calculation
        for (int i = 0; i < 10; i++) {
            Games historicalGame = new Games();
            historicalGame.setGameDate(LocalDate.now().minusDays(i + 1));
            historicalGame.setGameTime("7:00 PM ET");
            historicalGame.setStatus("FINAL");
            historicalGame.setExternalId(3001L + i);
            historicalGame.setHomeTeam(homeTeam);
            historicalGame.setAwayTeam(awayTeam);
            historicalGame.setSeason(2024);
            historicalGame = gamesRepository.save(historicalGame);
            
            GameStats stats = new GameStats();
            stats.setPlayer(testPlayer);
            stats.setGame(historicalGame);
            stats.setPoints(i < 8 ? 25 : 15);
            stats.setAssists(5 + (i % 3));
            stats.setRebounds(8 + (i % 4));
            stats.setMinutesPlayed("30:00");
            stats.setCreatedAt(LocalDate.now());
            gameStatsRepository.save(stats);
        }

        // First calculate confidence score and save it
        BigDecimal confidenceScore = confidenceScoreService.calculateConfidenceScore(
            testPlayer, 
            todayGame, 
            20, 
            StatCategory.POINTS
        );

        // Add debug logging to check the value
        System.out.println("Calculated confidence score: " + confidenceScore);

        // Make sure to convert to integer before setting
        UserPicks userPick = new UserPicks();
        userPick.setPlayer(testPlayer);
        userPick.setCategory(StatCategory.POINTS);
        userPick.setThreshold(20);
        userPick.setGame(todayGame);
        userPick.setHitRateAtPick(BigDecimal.valueOf(80.0));
        userPick.setConfidenceScore(confidenceScore != null ? confidenceScore.intValue() : 50); // Provide default value
        userPick.setCreatedAt(LocalDate.now());
        userPick.setUser(user);
        userPicksRepository.save(userPick);

        // Verify the response includes the confidence score
        mockMvc.perform(get("/api/dashboard/stats")
                .param("category", "POINTS")
                .param("threshold", "20")
                .param("timePeriod", "L10")
                .param("sortBy", "hitrate")
                .param("sortDirection", "desc"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].playerId").exists())
                .andExpect(jsonPath("$.data[0].playerName").exists())
                .andExpect(jsonPath("$.data[0].team").exists())
                .andExpect(jsonPath("$.data[0].hitRate").value(80.0))
                .andExpect(jsonPath("$.data[0].average").value(23.0))
                .andExpect(jsonPath("$.data[0].confidenceScore").exists()) 
                .andExpect(jsonPath("$.data[0].confidenceScore").isNumber())
                .andExpect(jsonPath("$.data[0].confidenceScore").value(Matchers.allOf(
                    Matchers.greaterThanOrEqualTo(0),
                    Matchers.lessThanOrEqualTo(100)
                )));
    }

    @Test
    @Transactional
    public void testResponseFormat() {
        // ... existing test code ...
    }
} 