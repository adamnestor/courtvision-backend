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

    private Players testPlayer;  // Add class field

    @BeforeEach
    void setUp() {
        // Clean up existing data
        gameStatsRepository.deleteAll();
        userPicksRepository.deleteAll();
        gamesRepository.deleteAll();
        playersRepository.deleteAll();
        teamsRepository.deleteAll();
        usersRepository.deleteAll();

        // Create test user with matching email
        Users user = new Users();
        user.setEmail("user");  // Match the default username from @WithMockUser
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

        // Create test pick with user
        UserPicks pick = new UserPicks();
        pick.setPlayer(testPlayer);
        pick.setGame(game);
        pick.setCategory(StatCategory.POINTS);
        pick.setThreshold(20);
        pick.setConfidenceScore(80);
        pick.setUser(user);
        pick.setCreatedAt(LocalDate.now());
        pick.setHitRateAtPick(new BigDecimal("80.00"));
        pick.setCreatedTime("12:00 PM");
        userPicksRepository.save(pick);

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
                .andExpect(jsonPath("$.data[0].confidenceScore").isNumber());
    }

    @Test
    @WithMockUser
    void dashboardResponse_ShouldMatchRequiredFormat() throws Exception {
        // First create some test data
        Teams testTeam = new Teams();
        testTeam.setName("Test Team");
        testTeam.setExternalId(555555L);
        testTeam.setAbbreviation("TT");
        testTeam.setCity("Test City");
        testTeam.setConference(Conference.East);
        testTeam.setDivision("Atlantic");
        testTeam = teamsRepository.save(testTeam);

        Players testPlayer = new Players();
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setStatus(PlayerStatus.ACTIVE);  // Add status if required
        testPlayer.setExternalId(666666L);  // Add required external_id
        testPlayer.setTeam(testTeam);
        testPlayer.setCreatedAt(LocalDate.now());  // Add required created_at
        testPlayer = playersRepository.save(testPlayer);

        // Create a game and stats
        Games game = new Games();
        game.setGameDate(LocalDate.now().minusDays(1));
        game.setHomeTeam(testTeam);
        game.setAwayTeam(testTeam);
        game.setStatus("FINAL");
        game.setExternalId(777777L);  // Add required external_id
        game.setSeason(2024);  // Add required season
        game = gamesRepository.save(game);

        GameStats stats = new GameStats();
        stats.setGame(game);
        stats.setPlayer(testPlayer);
        stats.setPoints(25);
        stats.setMinutesPlayed("30:00");  // Add if required
        gameStatsRepository.save(stats);

        // Now test the endpoint
        mockMvc.perform(get("/api/dashboard/stats")
                .param("category", "POINTS")
                .param("threshold", "20")
                .param("timePeriod", "L10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].playerId").exists())
                .andExpect(jsonPath("$.data[0].playerName").exists())
                .andExpect(jsonPath("$.data[0].team").exists())
                .andExpect(jsonPath("$.data[0].hitRate").isNumber())
                .andExpect(jsonPath("$.data[0].average").isNumber())
                .andExpect(jsonPath("$.data[0].confidenceScore").isNumber());
    }
} 