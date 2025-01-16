package com.adamnestor.courtvision.confidence.util;

import com.adamnestor.courtvision.confidence.model.RestImpact;
import com.adamnestor.courtvision.confidence.util.RestCalculator;
import com.adamnestor.courtvision.domain.Games;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestCalculatorTest {

    private static final int SCALE = 4;
    private List<Games> recentGames;
    private LocalDate baseDate;

    @BeforeEach
    void setUp() {
        recentGames = new ArrayList<>();
        baseDate = LocalDate.of(2024, 1, 1);

        // Create mock games
        for (int i = 0; i < 5; i++) {
            Games game = mock(Games.class);
            when(game.getGameDate()).thenReturn(baseDate.plusDays(i * 2));
            recentGames.add(game);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "2024-01-01, 2024-01-02, 1",
            "2024-01-01, 2024-01-03, 2",
            "2024-01-01, 2024-01-04, 3",
            "2024-01-01, 2024-01-01, 0"
    })
    void calculateDaysOfRest_WithValidDates_ReturnsCorrectDays(
            String previousDate, String currentDate, int expectedDays) {
        LocalDate prev = LocalDate.parse(previousDate);
        LocalDate curr = LocalDate.parse(currentDate);

        assertEquals(expectedDays, RestCalculator.calculateDaysOfRest(prev, curr));
    }

    @Test
    void calculateDaysOfRest_WithNullDates_ReturnsNegativeOne() {
        assertEquals(-1, RestCalculator.calculateDaysOfRest(null, LocalDate.now()));
        assertEquals(-1, RestCalculator.calculateDaysOfRest(LocalDate.now(), null));
        assertEquals(-1, RestCalculator.calculateDaysOfRest(null, null));
    }

    @Test
    void calculateDaysOfRest_WithInvalidDateOrder_ThrowsException() {
        LocalDate later = LocalDate.of(2024, 1, 2);
        LocalDate earlier = LocalDate.of(2024, 1, 1);

        assertThrows(IllegalArgumentException.class, () ->
                RestCalculator.calculateDaysOfRest(later, earlier)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void getRestMultiplier_WithValidDays_ReturnsCorrectMultiplier(int daysOfRest) {
        BigDecimal multiplier = RestCalculator.getRestMultiplier(daysOfRest);

        assertNotNull(multiplier);
        assertTrue(multiplier.compareTo(BigDecimal.ZERO) > 0);

        // Verify specific multipliers
        if (daysOfRest == 0) {
            assertEquals(new BigDecimal("0.90"), multiplier);
        } else if (daysOfRest == 1) {
            assertEquals(BigDecimal.ONE, multiplier);
        } else if (daysOfRest == 2) {
            assertEquals(new BigDecimal("1.02"), multiplier);
        } else {
            assertEquals(new BigDecimal("1.05"), multiplier);
        }
    }

    @Test
    void getRestMultiplier_WithNegativeDays_ReturnsBaselineMultiplier() {
        assertEquals(BigDecimal.ONE, RestCalculator.getRestMultiplier(-1));
    }

    @Test
    void calculateRestImpactScore_WithNullGames_ReturnsOne() {
        assertEquals(BigDecimal.ONE, RestCalculator.calculateRestImpactScore(null, 2));
    }

    @Test
    void calculateRestImpactScore_WithEmptyGames_ReturnsOne() {
        assertEquals(BigDecimal.ONE,
                RestCalculator.calculateRestImpactScore(new ArrayList<>(), 2));
    }

    @Test
    void calculateRestImpactScore_WithValidGames_ReturnsCalculatedScore() {
        BigDecimal score = RestCalculator.calculateRestImpactScore(recentGames, 2);

        assertNotNull(score);
        assertTrue(score.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(SCALE, score.scale());
    }

    @Test
    void createRestImpact_WithValidData_ReturnsCorrectImpact() {
        LocalDate previousGame = baseDate;
        LocalDate currentGame = baseDate.plusDays(2);

        RestImpact impact = RestCalculator.createRestImpact(
                previousGame, currentGame, recentGames);

        assertNotNull(impact);
        assertEquals(2, impact.getDaysOfRest());
        assertEquals(currentGame, impact.getGameDate());
        assertEquals(new BigDecimal("1.02"), impact.getMultiplier());
        assertTrue(impact.getImpactScore().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void createRestImpact_WithNoRecentGames_ReturnsBaselineImpact() {
        LocalDate previousGame = baseDate;
        LocalDate currentGame = baseDate.plusDays(2);

        RestImpact impact = RestCalculator.createRestImpact(
                previousGame, currentGame, new ArrayList<>());

        assertNotNull(impact);
        assertEquals(2, impact.getDaysOfRest());
        assertEquals(currentGame, impact.getGameDate());
        assertEquals(new BigDecimal("1.02"), impact.getMultiplier());
        assertEquals(BigDecimal.ONE, impact.getImpactScore());
    }

    @Test
    void createRestImpact_WithBackToBack_ReturnsReducedMultiplier() {
        LocalDate previousGame = baseDate;
        LocalDate currentGame = baseDate.plusDays(0);

        RestImpact impact = RestCalculator.createRestImpact(
                previousGame, currentGame, recentGames);

        assertNotNull(impact);
        assertEquals(0, impact.getDaysOfRest());
        assertEquals(new BigDecimal("0.90"), impact.getMultiplier());
    }

    @Test
    void createRestImpact_WithExtendedRest_ReturnsIncreasedMultiplier() {
        LocalDate previousGame = baseDate;
        LocalDate currentGame = baseDate.plusDays(4);

        RestImpact impact = RestCalculator.createRestImpact(
                previousGame, currentGame, recentGames);

        assertNotNull(impact);
        assertEquals(4, impact.getDaysOfRest());
        assertEquals(new BigDecimal("1.05"), impact.getMultiplier());
    }
}