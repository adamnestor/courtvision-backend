package com.adamnestor.courtvision.test.service.impl;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.mapper.DashboardMapper;
import com.adamnestor.courtvision.mapper.PlayerMapper;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.cache.StatsCacheService;
import com.adamnestor.courtvision.service.impl.HitRateCalculationServiceImpl;
import com.adamnestor.courtvision.service.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HitRateCalculationServiceImplTest {

    @Mock
    private GameStatsRepository gameStatsRepository;
    @Mock
    private GamesRepository gamesRepository;
    @Mock
    private StatsCacheService cacheService;
    @Mock
    private PlayersRepository playersRepository;
    @Mock
    private DashboardMapper dashboardMapper;
    @Mock
    private PlayerMapper playerMapper;
    @Mock
    private DateUtils dateUtils;

    private HitRateCalculationServiceImpl hitRateService;

    private Players testPlayer;
    private Teams testTeam;
    private Games testGame;
    private List<GameStats> testGameStats;

    @BeforeEach
    void setUp() {
        hitRateService = new HitRateCalculationServiceImpl(
                gameStatsRepository,
                gamesRepository,
                cacheService,
                playersRepository,
                dashboardMapper,
                playerMapper,
                dateUtils
        );

        // Setup test data
        testTeam = new Teams();
        testTeam.setId(1L);
        testTeam.setAbbreviation("TEST");
        testTeam.setName("Test Team");

        testPlayer = new Players();
        testPlayer.setId(1L);
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        testPlayer.setTeam(testTeam);
        testPlayer.setStatus(PlayerStatus.ACTIVE);

        testGame = new Games();
        testGame.setId(1L);
        testGame.setGameDate(LocalDateTime.now());
        testGame.setHomeTeam(testTeam);
        testGame.setStatus(GameStatus.SCHEDULED);

        testGameStats = createTestGameStats();
    }

    @Test
    void calculateHitRate_WithValidInputs_ShouldReturnCorrectCalculation() {
        // Arrange
        when(cacheService.getHitRate(any(), any(), any(), any())).thenReturn(null);
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(testGameStats);

        // Act
        Map<String, Object> result = hitRateService.calculateHitRate(
                testPlayer,
                StatCategory.POINTS,
                20,
                TimePeriod.L10
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("hitRate"));
        assertTrue(result.containsKey("average"));
    }

    @Test
    void calculateHitRate_WithNullPlayer_ShouldThrowException() {
        // Assert
        assertThrows(IllegalArgumentException.class, () ->
                hitRateService.calculateHitRate(null, StatCategory.POINTS, 20, TimePeriod.L10)
        );
    }

    @Test
    void getPlayerAverages_ShouldReturnCorrectAverages() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(testGameStats);

        // Act
        Map<StatCategory, BigDecimal> averages = hitRateService.getPlayerAverages(
                testPlayer,
                TimePeriod.L10
        );

        // Assert
        assertNotNull(averages);
        assertTrue(averages.containsKey(StatCategory.POINTS));
        assertTrue(averages.containsKey(StatCategory.ASSISTS));
        assertTrue(averages.containsKey(StatCategory.REBOUNDS));
    }

    @Test
    void hasSufficientData_WithEnoughGames_ShouldReturnTrue() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(testGameStats);

        // Act
        boolean result = hitRateService.hasSufficientData(testPlayer, TimePeriod.L5);

        // Assert
        assertTrue(result);
    }

    @Test
    void getDashboardStats_ShouldReturnCorrectStats() {
        // Arrange
        List<Games> todaysGames = Collections.singletonList(testGame);
        when(gamesRepository.findByGameDateAndStatus(any(), any())).thenReturn(todaysGames);
        when(playersRepository.findByTeamIdInAndStatus(anySet(), any())).thenReturn(Collections.singletonList(testPlayer));
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(testGameStats);

        DashboardStatsRow mockRow = new DashboardStatsRow(
                testPlayer.getId(),                                        // playerId
                testPlayer.getFirstName() + " " + testPlayer.getLastName(), // playerName
                testTeam.getAbbreviation(),                               // team
                "vs OPP",                                                 // opponent
                "Points 20+",                                             // statLine
                BigDecimal.valueOf(80.0),                                 // hitRate
                BigDecimal.valueOf(25.5)                                  // average
        );
        when(dashboardMapper.toStatsRow(any(), any(), any(), any(), any())).thenReturn(mockRow);

        // Act
        List<DashboardStatsRow> results = hitRateService.getDashboardStats(
                TimePeriod.L10,
                StatCategory.POINTS,
                20,
                "hitrate",
                "desc"
        );

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    void getPlayerDetailStats_ShouldReturnCorrectStats() {
        // Arrange
        when(playersRepository.findById(anyLong())).thenReturn(Optional.of(testPlayer));
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(testGameStats);
        PlayerDetailStats mockDetailStats = mock(PlayerDetailStats.class);
        when(playerMapper.toPlayerDetailStats(any(), any(), any(), any(), any(), any()))
                .thenReturn(mockDetailStats);

        // Act
        PlayerDetailStats result = hitRateService.getPlayerDetailStats(
                1L,
                TimePeriod.L10,
                StatCategory.POINTS,
                20
        );

        // Assert
        assertNotNull(result);
        verify(playerMapper).toPlayerDetailStats(
                eq(testPlayer),
                anyList(),
                anyMap(),
                eq(StatCategory.POINTS),
                eq(TimePeriod.L10),
                eq(20)
        );
    }

    @Test
    void getPlayerDetailStats_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(playersRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Assert
        assertThrows(IllegalArgumentException.class, () ->
                hitRateService.getPlayerDetailStats(999L, TimePeriod.L10, StatCategory.POINTS, 20)
        );
    }

    // Helper method to create test game stats
    private List<GameStats> createTestGameStats() {
        List<GameStats> stats = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GameStats gameStats = new GameStats();
            gameStats.setGame(testGame);
            gameStats.setPlayer(testPlayer);
            gameStats.setPoints(25);
            gameStats.setAssists(5);
            gameStats.setRebounds(8);
            stats.add(gameStats);
        }
        return stats;
    }
}