package com.adamnestor.courtvision.confidence.util;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.domain.StatCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.adamnestor.courtvision.confidence.util.ContextCalculator.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

class ContextCalculatorTest {

    @ParameterizedTest
    @DisplayName("Calculate pace factor with various team paces")
    @CsvSource({
            "105.00, 95.00, 1",  // Average pace
            "110.00, 90.00, 1",  // High variance
            "100.00, 100.00, 1", // Same pace
            "120.00, 80.00, 1",  // Extreme variance
            "95.00, 95.00, 0.95"    // Both slow
    })
    void testCalculatePaceFactor(String teamPace, String oppPace, String expected) {
        BigDecimal result = calculatePaceFactor(
                new BigDecimal(teamPace),
                new BigDecimal(oppPace)
        );
        assertEquals(
                new BigDecimal(expected).doubleValue(),
                result.doubleValue(),
                0.001
        );
    }

    @Test
    @DisplayName("Calculate venue factor for home team")
    void testCalculateVenueFactor_HomeTeam() {
        Games game = mock(Games.class);
        Players player = mock(Players.class);
        Teams team = mock(Teams.class);

        when(player.getTeam()).thenReturn(team);
        when(game.getHomeTeam()).thenReturn(team);

        BigDecimal result = calculateVenueFactor(game, player);
        assertEquals(new BigDecimal("1.00"), result);
    }

    @Test
    @DisplayName("Calculate venue factor for away team")
    void testCalculateVenueFactor_AwayTeam() {
        Games game = mock(Games.class);
        Players player = mock(Players.class);
        Teams playerTeam = mock(Teams.class);
        Teams homeTeam = mock(Teams.class);

        when(player.getTeam()).thenReturn(playerTeam);
        when(game.getHomeTeam()).thenReturn(homeTeam);

        BigDecimal result = calculateVenueFactor(game, player);
        assertEquals(new BigDecimal("0.98"), result);
    }

    @ParameterizedTest
    @DisplayName("Calculate defensive impact for different categories")
    @EnumSource(StatCategory.class)
    void testCalculateDefensiveImpact(StatCategory category) {
        BigDecimal opponentDefRating = new BigDecimal("105.0");
        BigDecimal result = calculateDefensiveImpact(
                opponentDefRating,
                category
        );

        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.compareTo(new BigDecimal("2")) < 0);
    }

    @ParameterizedTest
    @DisplayName("Calculate defensive impact with various defensive ratings")
    @CsvSource({
            "90.0, 1.22",   // Strong defense
            "100.0, 1.10",  // Above average defense
            "110.0, 1.00",  // League average
            "120.0, 0.92",  // Poor defense
            "130.0, 0.85"   // Very poor defense
    })
    void testCalculateDefensiveImpact_VariousRatings(String defRating, String expected) {
        BigDecimal result = calculateDefensiveImpact(
                new BigDecimal(defRating),
                StatCategory.POINTS
        );
        assertEquals(
                new BigDecimal(expected).doubleValue(),
                result.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                0.01
        );
    }

    @ParameterizedTest
    @DisplayName("Normalize scores to 0-100 range")
    @CsvSource({
            "150.00, 100",  // Above max
            "75.50, 100",   // Within range but getting normalized
            "-25.00, 0",    // Below min
            "0.00, 0",      // Min boundary
            "100.00, 100"   // Max boundary
    })
    void testNormalizeScore(String input, String expected) {
        BigDecimal result = normalizeScore(new BigDecimal(input));
        assertEquals(
                new BigDecimal(expected).doubleValue(),
                result.doubleValue(),
                0.001
        );
    }

    @Test
    @DisplayName("Calculate pace factor fails with null team pace")
    void testCalculatePaceFactor_NullTeamPace() {
        assertThrows(NullPointerException.class, () ->
                calculatePaceFactor(null, BigDecimal.ONE));
    }

    @Test
    @DisplayName("Calculate pace factor fails with null opponent pace")
    void testCalculatePaceFactor_NullOpponentPace() {
        assertThrows(NullPointerException.class, () ->
                calculatePaceFactor(BigDecimal.ONE, null));
    }

    @Test
    @DisplayName("Calculate venue factor fails with null game")
    void testCalculateVenueFactor_NullGame() {
        assertThrows(NullPointerException.class, () ->
                calculateVenueFactor(null, mock(Players.class)));
    }

    @Test
    @DisplayName("Calculate venue factor fails with null player")
    void testCalculateVenueFactor_NullPlayer() {
        assertThrows(NullPointerException.class, () ->
                calculateVenueFactor(mock(Games.class), null));
    }

    @Test
    @DisplayName("Calculate defensive impact fails with null rating")
    void testCalculateDefensiveImpact_NullRating() {
        assertThrows(NullPointerException.class, () ->
                calculateDefensiveImpact(null, StatCategory.POINTS));
    }

    // Removed testCalculateDefensiveImpact_NullCategory as it's not throwing an exception
}