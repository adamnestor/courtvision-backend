package com.adamnestor.courtvision.test.confidence.util;

import com.adamnestor.courtvision.confidence.util.BlowoutCalculator;
import com.adamnestor.courtvision.domain.GameStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

class BlowoutCalculatorTest {

    @Test
    @DisplayName("Calculate team strength differential with positive values")
    void testCalculateTeamStrengthDifferential_PositiveValues() {
        BigDecimal homeNetRating = new BigDecimal("5.5");
        BigDecimal awayNetRating = new BigDecimal("2.0");
        BigDecimal homePace = new BigDecimal("102.0");
        BigDecimal awayPace = new BigDecimal("98.0");

        BigDecimal result = BlowoutCalculator.calculateTeamStrengthDifferential(
                homeNetRating, awayNetRating, homePace, awayPace);

        assertEquals(new BigDecimal("7.4000"), result);
    }

    @ParameterizedTest
    @DisplayName("Test blowout detection with various score differences")
    @CsvSource({
            "120, 95, true",   // 25 point diff
            "95, 120, true",   // 25 point diff reversed
            "100, 90, false",  // 10 point diff
            "105, 85, true",   // 20 point diff (edge case)
            "110, 91, false"   // 19 point diff (edge case)
    })
    void testWasBlowout(Integer homeScore, Integer awayScore, boolean expected) {
        assertEquals(expected, BlowoutCalculator.wasBlowout(homeScore, awayScore));
    }

    @Test
    @DisplayName("Test blowout detection with null scores")
    void testWasBlowout_NullScores() {
        assertFalse(BlowoutCalculator.wasBlowout(null, 90));
        assertFalse(BlowoutCalculator.wasBlowout(90, null));
        assertFalse(BlowoutCalculator.wasBlowout(null, null));
    }

    @ParameterizedTest
    @DisplayName("Calculate performance retention with various PIE and usage values")
    @MethodSource("providePerformanceRetentionData")
    void testCalculatePerformanceRetention(
            BigDecimal pie,
            BigDecimal usageRate,
            BigDecimal expected) {
        BigDecimal result = BlowoutCalculator.calculatePerformanceRetention(pie, usageRate);
        // Compare raw values to handle scale differences
        assertEquals(expected.stripTrailingZeros(), result.stripTrailingZeros());
    }

    private static Stream<Arguments> providePerformanceRetentionData() {
        return Stream.of(
                Arguments.of(
                        new BigDecimal("0.150"),  // 1.5x league average
                        new BigDecimal("25.00"),  // 1.25x league average
                        BigDecimal.ONE            // Capped at 1
                ),
                Arguments.of(
                        new BigDecimal("0.050"),  // 0.5x league average
                        new BigDecimal("10.00"),  // 0.5x league average
                        new BigDecimal("0.5000")  // At minimum value
                ),
                Arguments.of(
                        new BigDecimal("0.100"),  // League average
                        new BigDecimal("20.00"),  // League average
                        BigDecimal.ONE            // (1.0 * 0.6) + (1.0 * 0.4) = 1.0
                ),
                Arguments.of(
                        null,
                        new BigDecimal("20.00"),
                        BigDecimal.ONE            // Null PIE returns 1
                ),
                Arguments.of(
                        new BigDecimal("0.100"),
                        null,
                        BigDecimal.ONE            // Null usage returns 1
                )
        );
    }

    @ParameterizedTest
    @DisplayName("Calculate blowout probability with various strength differentials")
    @CsvSource({
            "10.0, 73.1059",  // Strong favorite
            "0.0, 50.0000",   // Even matchup
            "-10.0, 26.8941", // Strong underdog
            "20.0, 88.0797",  // Heavy favorite
            "-20.0, 11.9203"  // Heavy underdog
    })
    void testCalculateBlowoutProbability(double strengthDiff, double expectedProb) {
        BigDecimal result = BlowoutCalculator.calculateBlowoutProbability(
                new BigDecimal(String.valueOf(strengthDiff)));

        assertEquals(
                new BigDecimal(String.valueOf(expectedProb)).setScale(4, java.math.RoundingMode.HALF_UP),
                result,
                "Blowout probability calculation should match expected value"
        );
    }

    @Test
    @DisplayName("Calculate minutes retention in blowout vs normal games")
    void testCalculateMinutesRetention() {
        List<GameStats> blowoutGames = List.of(
                createGameStats("32:00"),
                createGameStats("28:00")
        );

        List<GameStats> normalGames = List.of(
                createGameStats("36:00"),
                createGameStats("35:00")
        );

        BigDecimal result = BlowoutCalculator.calculateMinutesRetention(blowoutGames, normalGames);
        assertEquals(new BigDecimal("0.8451"), result);
    }

    @Test
    @DisplayName("Calculate minutes retention with empty game lists")
    void testCalculateMinutesRetention_EmptyLists() {
        assertEquals(BigDecimal.ONE,
                BlowoutCalculator.calculateMinutesRetention(List.of(), List.of()));
        assertEquals(BigDecimal.ONE,
                BlowoutCalculator.calculateMinutesRetention(List.of(), List.of(createGameStats("36:00"))));
        assertEquals(BigDecimal.ONE,
                BlowoutCalculator.calculateMinutesRetention(List.of(createGameStats("32:00")), List.of()));
    }

    private static GameStats createGameStats(String minutes) {
        GameStats stats = mock(GameStats.class);
        when(stats.getMinutesPlayed()).thenReturn(minutes);
        return stats;
    }
}