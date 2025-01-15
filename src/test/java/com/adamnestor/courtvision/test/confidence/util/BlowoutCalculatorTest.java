package com.adamnestor.courtvision.test.confidence.util;

import com.adamnestor.courtvision.confidence.util.BlowoutCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class BlowoutCalculatorTest {

    private static final int SCALE = 4;

    @Test
    void calculateTeamStrengthDifferential_EvenlyMatchedTeams_ReturnsSmallDifferential() {
        // Arrange
        BigDecimal homeNetRating = new BigDecimal("105.0");
        BigDecimal awayNetRating = new BigDecimal("103.0");
        BigDecimal homePace = new BigDecimal("100.0");
        BigDecimal awayPace = new BigDecimal("99.0");

        // Act
        BigDecimal differential = BlowoutCalculator.calculateTeamStrengthDifferential(
                homeNetRating, awayNetRating, homePace, awayPace);

        // Assert
        assertTrue(differential.compareTo(new BigDecimal("5.0")) < 0,
                "Differential should be small for evenly matched teams");
        assertTrue(differential.compareTo(BigDecimal.ZERO) > 0,
                "Differential should be positive for stronger home team");
    }

    @Test
    void calculateTeamStrengthDifferential_StrongVsWeakTeam_ReturnsLargeDifferential() {
        // Arrange
        BigDecimal homeNetRating = new BigDecimal("115.0");
        BigDecimal awayNetRating = new BigDecimal("95.0");
        BigDecimal homePace = new BigDecimal("102.0");
        BigDecimal awayPace = new BigDecimal("98.0");

        // Act
        BigDecimal differential = BlowoutCalculator.calculateTeamStrengthDifferential(
                homeNetRating, awayNetRating, homePace, awayPace);

        // Assert
        assertTrue(differential.compareTo(new BigDecimal("15.0")) > 0,
                "Differential should be large for mismatched teams");
    }

    @Test
    void calculateTeamStrengthDifferential_VerifyHomeAdvantage() {
        // Arrange
        BigDecimal sameRating = new BigDecimal("100.0");
        BigDecimal samePace = new BigDecimal("100.0");

        // Act
        BigDecimal differential = BlowoutCalculator.calculateTeamStrengthDifferential(
                sameRating, sameRating, samePace, samePace);

        // Assert
        assertEquals(new BigDecimal("1.5").setScale(SCALE, RoundingMode.HALF_UP),
                differential,
                "With identical teams, differential should equal home court advantage");
    }

    @ParameterizedTest
    @CsvSource({
            "120, 95, true",   // 25 point difference
            "105, 100, false", // 5 point difference
            "98, 95, false",   // 3 point difference
            "115, 90, true"    // 25 point difference
    })
    void wasBlowout_VariousScores_CorrectlyIdentifiesBlowouts(
            int homeScore, int awayScore, boolean expectedResult) {
        assertEquals(expectedResult,
                BlowoutCalculator.wasBlowout(homeScore, awayScore));
    }

    @Test
    void calculateBlowoutProbability_EvenlyMatchedTeams_ReturnsBaseProbability() {
        // Arrange
        BigDecimal smallDifferential = new BigDecimal("3.51"); // From our evenly matched scenario

        // Act
        BigDecimal probability = BlowoutCalculator.calculateBlowoutProbability(smallDifferential);

        // Assert
        assertTrue(probability.compareTo(new BigDecimal("35.0")) < 0,
                "Probability should be close to base rate for evenly matched teams");
        assertTrue(probability.compareTo(new BigDecimal("25.0")) > 0,
                "Probability should be above minimum threshold");
    }

    @Test
    void calculateBlowoutProbability_StrongMismatch_ReturnsHighProbability() {
        // Arrange
        BigDecimal largeDifferential = new BigDecimal("21.52"); // From our mismatched scenario

        // Act
        BigDecimal probability = BlowoutCalculator.calculateBlowoutProbability(largeDifferential);

        // Assert
        assertTrue(probability.compareTo(new BigDecimal("55.0")) > 0,
                "Probability should be high for mismatched teams");
        assertTrue(probability.compareTo(new BigDecimal("85.0")) <= 0,
                "Probability should not exceed maximum threshold");
    }

    @Test
    void calculateHistoricalMatchupFactor_NoBlowouts_ReturnsBaselineMultiplier() {
        // Act
        BigDecimal factor = BlowoutCalculator.calculateHistoricalMatchupFactor(0, 10);

        // Assert
        assertEquals(0, factor.compareTo(BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP)),
                "Factor should be 1.0 when no historical blowouts");
    }

    @Test
    void calculateHistoricalMatchupFactor_AllBlowouts_ReturnsMaximumMultiplier() {
        // Act
        BigDecimal factor = BlowoutCalculator.calculateHistoricalMatchupFactor(10, 10);

        // Assert
        assertEquals(0, factor.compareTo(new BigDecimal("1.05").setScale(SCALE, RoundingMode.HALF_UP)),
                "Factor should be 1.05 (5% increase) when all games were blowouts");
    }

    @Test
    void calculateHistoricalMatchupFactor_NoGames_ReturnsOne() {
        // Act
        BigDecimal factor = BlowoutCalculator.calculateHistoricalMatchupFactor(0, 0);

        // Assert
        assertEquals(0, factor.compareTo(BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP)),
                "Factor should be 1.0 when no historical games");
    }

    @Test
    void calculateHistoricalMatchupFactor_PartialBlowouts_ReturnsProportionalMultiplier() {
        // Act
        BigDecimal factor = BlowoutCalculator.calculateHistoricalMatchupFactor(5, 10);

        // Assert
        BigDecimal expected = BigDecimal.ONE.add(
                        new BigDecimal("0.5").multiply(new BigDecimal("0.05")))
                .setScale(SCALE, RoundingMode.HALF_UP);
        assertEquals(0, factor.compareTo(expected),
                "Factor should be proportional to blowout ratio");
    }
}