package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DashboardMapperTest {

    private DashboardMapper mapper;
    private Players testPlayer;
    private Teams testTeam;
    private Map<String, Object> testStats;

    @BeforeEach
    void setUp() {
        mapper = new DashboardMapper();

        // Setup test team
        testTeam = new Teams();
        testTeam.setId(1L);
        testTeam.setAbbreviation("GSW");

        // Setup test player
        testPlayer = new Players();
        testPlayer.setId(1L);
        testPlayer.setFirstName("Stephen");
        testPlayer.setLastName("Curry");
        testPlayer.setTeam(testTeam);

        // Setup test stats
        testStats = new HashMap<>();
        testStats.put("hitRate", new BigDecimal("75.00"));
        testStats.put("average", new BigDecimal("28.5"));
    }

    @Test
    void testBasicMapping() {
        // Arrange
        String opponent = "LAL";

        // Act
        DashboardStatsRow result = mapper.toStatsRow(
                testPlayer,
                testStats,
                StatCategory.POINTS,
                20,
                opponent
        );

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.playerId());
        assertEquals("Stephen Curry", result.playerName());
        assertEquals("GSW", result.team());
        assertEquals("LAL", result.opponent());
        assertEquals("Points 20+", result.statLine());
        assertEquals(new BigDecimal("75.00"), result.hitRate());
        assertEquals(new BigDecimal("28.5"), result.average());
    }

    @ParameterizedTest
    @MethodSource("provideStatCategoriesAndThresholds")
    void testStatLineFormatting(StatCategory category, Integer threshold, String expected) {
        // Act
        DashboardStatsRow result = mapper.toStatsRow(
                testPlayer,
                testStats,
                category,
                threshold,
                "LAL"
        );

        // Assert
        assertEquals(expected, result.statLine());
    }

    private static Stream<Arguments> provideStatCategoriesAndThresholds() {
        return Stream.of(
                Arguments.of(StatCategory.POINTS, 20, "Points 20+"),
                Arguments.of(StatCategory.ASSISTS, 8, "Assists 8+"),
                Arguments.of(StatCategory.REBOUNDS, 10, "Rebounds 10+"),
                Arguments.of(StatCategory.ALL, null, ""),
                Arguments.of(StatCategory.POINTS, null, "")
        );
    }

    @Test
    void testWithNullStats() {
        // Arrange
        Map<String, Object> nullStats = new HashMap<>();
        nullStats.put("hitRate", null);
        nullStats.put("average", null);

        // Act
        DashboardStatsRow result = mapper.toStatsRow(
                testPlayer,
                nullStats,
                StatCategory.POINTS,
                20,
                "LAL"
        );

        // Assert
        assertNotNull(result);
        assertNull(result.hitRate());
        assertNull(result.average());
    }

    @Test
    void testWithNullPlayer() {
        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                mapper.toStatsRow(
                        null,
                        testStats,
                        StatCategory.POINTS,
                        20,
                        "LAL"
                )
        );
    }

    @Test
    void testWithNullTeam() {
        // Arrange
        Players playerWithoutTeam = new Players();
        playerWithoutTeam.setId(1L);
        playerWithoutTeam.setFirstName("Stephen");
        playerWithoutTeam.setLastName("Curry");

        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                mapper.toStatsRow(
                        playerWithoutTeam,
                        testStats,
                        StatCategory.POINTS,
                        20,
                        "LAL"
                )
        );
    }
}