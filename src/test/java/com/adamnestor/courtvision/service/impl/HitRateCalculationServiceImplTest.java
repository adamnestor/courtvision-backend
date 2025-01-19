package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.mapper.DashboardMapper;
import com.adamnestor.courtvision.mapper.PlayerMapper;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HitRateCalculationServiceImplTest {

    @Mock(lenient = true)
    private GameStatsRepository gameStatsRepository;
    @Mock
    private GamesRepository gamesRepository;
    @Mock
    private PlayersRepository playersRepository;
    @Mock
    private DashboardMapper dashboardMapper;
    @Mock(lenient = true)
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

        // Create away team
        Teams awayTeam = new Teams();
        awayTeam.setId(2L);
        awayTeam.setAbbreviation("OPP");
        awayTeam.setName("Opponent Team");

        testGame = new Games();
        testGame.setId(1L);
        testGame.setGameDate(LocalDate.now());
        testGame.setGameTime("7:00 PM ET");
        testGame.setHomeTeam(testTeam);
        testGame.setAwayTeam(awayTeam);
        testGame.setStatus("SCHEDULED");

        testGameStats = createTestGameStats();
        
        // Setup default mock behavior
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(testGameStats);
    }

    @Test
    void calculateHitRate_WithValidInputs_ShouldReturnCorrectCalculation() {
        // Arrange
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
        
        BigDecimal hitRate = (BigDecimal) result.get("hitRate");
        BigDecimal average = (BigDecimal) result.get("average");
        
        // Use compareTo for BigDecimal comparison
        assertTrue(hitRate.compareTo(BigDecimal.valueOf(50.0)) == 0, 
                "Hit rate should be 50.0% (5 out of 10 games above threshold)");
        assertTrue(average.compareTo(BigDecimal.valueOf(20.0)) == 0,
                "Average should be 20.0 ((25 * 5 + 15 * 5) / 10)");
        
        verify(gameStatsRepository).findPlayerRecentGames(any());
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
        List<GameStats> fiveGames = testGameStats.subList(0, 5);
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(fiveGames);

        // Act
        boolean result = hitRateService.hasSufficientData(testPlayer, TimePeriod.L5);

        // Assert
        assertTrue(result);
        verify(gameStatsRepository).findPlayerRecentGames(any());
    }

    @Test
    void hasSufficientData_WithInsufficientGames_ShouldReturnFalse() {
        // Arrange
        List<GameStats> threeGames = testGameStats.subList(0, 3);
        // Clear default mock behavior
        reset(gameStatsRepository);
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(threeGames);

        // Act
        boolean result = hitRateService.hasSufficientData(testPlayer, TimePeriod.L5);

        // Assert
        assertFalse(result, "Should return false when there are fewer games than required");
        verify(gameStatsRepository).findPlayerRecentGames(any());
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

    @Test
    void calculateHitRate_WithCachedResult_ShouldReturnCachedValue() {
        // Arrange
        Map<String, Object> cachedResult = Map.of(
            "hitRate", BigDecimal.valueOf(75.0),
            "average", BigDecimal.valueOf(22.5)
        );
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
        assertEquals(cachedResult, result);
        verify(gameStatsRepository, never()).findPlayerRecentGames(any());
    }

    @Test
    void calculateHitRate_WithInvalidThreshold_ShouldThrowException() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () ->
                hitRateService.calculateHitRate(testPlayer, StatCategory.POINTS, 0, TimePeriod.L10)),
            () -> assertThrows(IllegalArgumentException.class, () ->
                hitRateService.calculateHitRate(testPlayer, StatCategory.POINTS, 52, TimePeriod.L10)),
            () -> assertThrows(IllegalArgumentException.class, () ->
                hitRateService.calculateHitRate(testPlayer, StatCategory.POINTS, null, TimePeriod.L10))
        );
    }

    @Test
    void getPlayerGames_ShouldRespectTimePeriodLimit() {
        // Arrange
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(testGameStats);
        
        // Act
        Map<String, Object> resultL5 = hitRateService.calculateHitRate(
            testPlayer,
            StatCategory.POINTS,
            20,
            TimePeriod.L5
        );
        
        Map<String, Object> resultL10 = hitRateService.calculateHitRate(
            testPlayer,
            StatCategory.POINTS,
            20,
            TimePeriod.L10
        );

        // Assert
        verify(gameStatsRepository, times(2)).findPlayerRecentGames(any());
        assertNotNull(resultL5);
        assertNotNull(resultL10);
        // Verify the results are different due to different periods
        assertNotEquals(resultL5.get("hitRate"), resultL10.get("hitRate"));
    }

    @Test
    void getDashboardStats_WithAllCategories_ShouldReturnAllStats() {
        // Arrange
        List<Games> todaysGames = Collections.singletonList(testGame);
        when(gamesRepository.findByGameDateAndStatus(any(), any())).thenReturn(todaysGames);
        when(playersRepository.findByTeamIdInAndStatus(anySet(), any()))
            .thenReturn(Collections.singletonList(testPlayer));
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(testGameStats);
        
        DashboardStatsRow mockRow = new DashboardStatsRow(
            testPlayer.getId(),
            testPlayer.getFirstName() + " " + testPlayer.getLastName(),
            testTeam.getAbbreviation(),
            "vs OPP",
            "Points 20+",
            BigDecimal.valueOf(80.0),
            BigDecimal.valueOf(25.5)
        );
        when(dashboardMapper.toStatsRow(any(), any(), any(), any(), any())).thenReturn(mockRow);

        // Act
        List<DashboardStatsRow> results = hitRateService.getDashboardStats(
            TimePeriod.L10,
            StatCategory.ALL,
            null,
            "hitrate",
            "desc"
        );

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        // Should have 3 entries (one for each stat category)
        assertEquals(3, results.size());
        verify(dashboardMapper, times(3)).toStatsRow(any(), any(), any(), any(), any());
    }

    @Test
    void getDashboardStats_WithEmptyGames_ShouldReturnEmptyList() {
        // Arrange
        when(gamesRepository.findByGameDateAndStatus(any(), any())).thenReturn(Collections.emptyList());

        // Act
        List<DashboardStatsRow> results = hitRateService.getDashboardStats(
            TimePeriod.L10,
            StatCategory.POINTS,
            20,
            "hitrate",
            "desc"
        );

        // Assert
        assertTrue(results.isEmpty());
        verify(playersRepository, never()).findByTeamIdInAndStatus(anySet(), any());
    }

    @Test
    void sortStats_ShouldRespectSortingParameters() {
        // Arrange
        List<Games> todaysGames = Collections.singletonList(testGame);
        when(gamesRepository.findByGameDateAndStatus(any(), any())).thenReturn(todaysGames);
        when(playersRepository.findByTeamIdInAndStatus(anySet(), any()))
            .thenReturn(Collections.singletonList(testPlayer));
        when(gameStatsRepository.findPlayerRecentGames(any())).thenReturn(testGameStats);

        // Act & Assert
        // Test ascending order
        List<DashboardStatsRow> ascResults = hitRateService.getDashboardStats(
            TimePeriod.L10,
            StatCategory.POINTS,
            20,
            "average",
            "asc"
        );
        assertNotNull(ascResults);

        // Test descending order
        List<DashboardStatsRow> descResults = hitRateService.getDashboardStats(
            TimePeriod.L10,
            StatCategory.POINTS,
            20,
            "average",
            "desc"
        );
        assertNotNull(descResults);
    }

    // Helper method to create test game stats
    private List<GameStats> createTestGameStats() {
        List<GameStats> stats = new ArrayList<>();
        
        // Create 10 games with consistent stats pattern
        for (int i = 0; i < 10; i++) {
            GameStats gameStats = new GameStats();
            gameStats.setId((long) i);
            gameStats.setGame(testGame);
            gameStats.setPlayer(testPlayer);
            
            if (i < 5) {
                // First 5 games above threshold
                gameStats.setPoints(25);
                gameStats.setAssists(8);
                gameStats.setRebounds(12);
            } else {
                // Last 5 games below threshold
                gameStats.setPoints(15);
                gameStats.setAssists(3);
                gameStats.setRebounds(5);
            }
            
            // Set other required fields
            gameStats.setMinutesPlayed("30");
            stats.add(gameStats);
        }
        return stats;
    }
}