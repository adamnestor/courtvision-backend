package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.dto.picks.UserPickDTO;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.response.DashboardStatsResponse;
import com.adamnestor.courtvision.dto.response.PickResponse;
import com.adamnestor.courtvision.dto.response.PlayerStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResponseMapperTest {

    private ResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ResponseMapper();
    }

    @Test
    void testToPlayerStatsResponse() {
        // Arrange
        PlayerDetailStats stats = new PlayerDetailStats(
            1L,
            "Stephen Curry",
            "GSW",
            new BigDecimal("75.00"),
            85,
            20,
            new BigDecimal("28.5")
        );

        // Act
        PlayerStatsResponse response = mapper.toPlayerStatsResponse(stats);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.playerId());
        assertEquals("Stephen Curry", response.playerName());
        assertEquals("GSW", response.team());
        assertEquals(new BigDecimal("75.00"), response.hitRate());
        assertEquals(85, response.confidenceScore());
        assertEquals(20, response.gamesPlayed());
        assertEquals(new BigDecimal("28.5"), response.average());
        assertTrue(response.isHighConfidence());
    }

    @Test
    void testToPickResponse() {
        // Arrange
        LocalDate now = LocalDate.now();
        Games game = new Games();
        game.setGameTime("7:00 PM ET");

        UserPickDTO pick = new UserPickDTO(
            1L,
            2L,
            "LeBron James",
            "LAL",
            "GSW",
            StatCategory.POINTS,
            Integer.valueOf(20),
            new BigDecimal("70.00"),
            Integer.valueOf(75),
            true,
            now,
            game
        );

        // Act
        PickResponse response = mapper.toPickResponse(pick);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(2L, response.playerId());
        assertEquals("LeBron James", response.playerName());
        assertEquals("LAL", response.team());
        assertEquals("GSW", response.opponent());
        assertEquals("POINTS", response.category());
        assertEquals(new BigDecimal("70.00"), response.hitRateAtPick());
        assertEquals(75, response.confidenceScore());
        assertEquals("WIN", response.result());
        assertNotNull(response.createdAt());
        assertEquals("7:00 PM ET", response.gameTime());
    }

    @Test
    void testToDashboardResponse() {
        // Arrange
        List<Integer> lastGames = Arrays.asList(1, 1, 0, 1, 1);
        DashboardStatsRow stats = new DashboardStatsRow(
            1L,
            "Kevin Durant",
            "PHX",
            StatCategory.POINTS,
            new BigDecimal("80.00"),
            90,
            15,
            new BigDecimal("32.5"),
            lastGames
        );

        // Act
        DashboardStatsResponse response = mapper.toDashboardResponse(stats);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.playerId());
        assertEquals("Kevin Durant", response.playerName());
        assertEquals("PHX", response.team());
        assertEquals("POINTS", response.category());
        assertEquals(new BigDecimal("80.00"), response.hitRate());
        assertEquals(90, response.confidenceScore());
        assertEquals(15, response.gamesPlayed());
        assertEquals(new BigDecimal("32.5"), response.average());
        assertEquals(lastGames, response.lastGames());
        assertTrue(response.isHighConfidence());
    }

    @Test
    void testToPlayerStatsResponseWithLowConfidence() {
        // Arrange
        PlayerDetailStats stats = new PlayerDetailStats(
            1L,
            "Player Name",
            "TEAM",
            new BigDecimal("60.00"),
            75, // Below 80 threshold
            10,
            new BigDecimal("15.5")
        );

        // Act
        PlayerStatsResponse response = mapper.toPlayerStatsResponse(stats);

        // Assert
        assertNotNull(response);
        assertFalse(response.isHighConfidence());
    }

    @Test
    void testToPickResponseWithLoss() {
        // Arrange
        LocalDate now = LocalDate.now();
        Games game = new Games();
        game.setGameTime("7:00 PM ET");

        UserPickDTO pick = new UserPickDTO(
            1L,
            2L,
            "Player Name",
            "TEAM",
            "OPP",
            StatCategory.POINTS,
            Integer.valueOf(20),
            new BigDecimal("70.00"),
            Integer.valueOf(75),
            false,
            now,
            game
        );

        // Act
        PickResponse response = mapper.toPickResponse(pick);

        // Assert
        assertNotNull(response);
        assertEquals("LOSS", response.result());
    }
} 