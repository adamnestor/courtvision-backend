package com.adamnestor.courtvision.test.service.util;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.service.util.StatAnalysisUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StatAnalysisUtilsTest {

    @Test
    void analyzeCategoryStats_WithValidData() {
        List<GameStats> games = createTestGames(10, 20, 5, 8); // 10 games, 20 pts, 5 ast, 8 reb

        Map<String, Object> analysis = StatAnalysisUtils.analyzeCategoryStats(games, StatCategory.POINTS);

        assertThat(analysis)
                .containsKey("average")
                .containsKey("category")
                .containsKey("thresholdAnalysis");

        assertThat(analysis.get("category")).isEqualTo(StatCategory.POINTS);
        assertThat(analysis.get("average")).isInstanceOf(BigDecimal.class);

        @SuppressWarnings("unchecked")
        Map<Integer, Map<String, Object>> thresholdAnalysis =
                (Map<Integer, Map<String, Object>>) analysis.get("thresholdAnalysis");

        assertThat(thresholdAnalysis).containsKeys(10, 15, 20, 25);
    }

    @Test
    void analyzeThreshold_WithValidData() {
        List<GameStats> games = createTestGames(10, 22, 5, 8); // All games above 20 points

        Map<String, Object> analysis = StatAnalysisUtils.analyzeThreshold(games, StatCategory.POINTS, 20);

        assertThat(analysis)
                .containsEntry("threshold", 20)
                .containsEntry("successCount", 10)
                .containsEntry("failureCount", 0);

        assertThat((BigDecimal) analysis.get("hitRate"))
                .isEqualByComparingTo("100.00");
        assertThat((BigDecimal) analysis.get("average"))
                .isEqualByComparingTo("22.00");
    }

    @Test
    void analyzeThreshold_WithMixedResults() {
        List<GameStats> games = createMixedGames(); // 6 successes, 4 failures for 20+ points

        Map<String, Object> analysis = StatAnalysisUtils.analyzeThreshold(games, StatCategory.POINTS, 20);

        assertThat(analysis)
                .containsEntry("successCount", 6)
                .containsEntry("failureCount", 4);

        assertThat((BigDecimal) analysis.get("hitRate"))
                .isEqualByComparingTo("60.00");
    }

    @Test
    void calculateHitRate_WithAllSuccesses() {
        List<GameStats> games = createTestGames(5, 25, 5, 8);

        BigDecimal hitRate = StatAnalysisUtils.calculateHitRate(games, StatCategory.POINTS, 20);

        assertThat(hitRate).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateHitRate_WithNoSuccesses() {
        List<GameStats> games = createTestGames(5, 15, 5, 8);

        BigDecimal hitRate = StatAnalysisUtils.calculateHitRate(games, StatCategory.POINTS, 20);

        assertThat(hitRate).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateHitRate_WithEmptyGamesList() {
        BigDecimal hitRate = StatAnalysisUtils.calculateHitRate(
                Collections.emptyList(), StatCategory.POINTS, 20);

        assertThat(hitRate).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateHitRate_WithNullGamesList() {
        BigDecimal hitRate = StatAnalysisUtils.calculateHitRate(null, StatCategory.POINTS, 20);

        assertThat(hitRate).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @EnumSource(StatCategory.class)
    void calculateAverage_ForAllCategories(StatCategory category) {
        List<GameStats> games = createTestGames(5, 20, 5, 8);

        BigDecimal average = StatAnalysisUtils.calculateAverage(games, category);

        assertThat(average).isGreaterThan(BigDecimal.ZERO);
        assertThat(average.scale()).isLessThanOrEqualTo(2);
    }

    @Test
    void calculateAverage_WithEmptyGamesList() {
        BigDecimal average = StatAnalysisUtils.calculateAverage(
                Collections.emptyList(), StatCategory.POINTS);

        assertThat(average).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @EnumSource(StatCategory.class)
    void getThresholdsForCategory_ReturnsValidThresholds(StatCategory category) {
        List<Integer> thresholds = StatAnalysisUtils.getThresholdsForCategory(category);

        assertThat(thresholds)
                .isNotEmpty()
                .doesNotContainNull()
                .allMatch(t -> t > 0);
    }

    @ParameterizedTest
    @MethodSource("provideValidThresholds")
    void isValidThreshold_WithValidThresholds(StatCategory category, int threshold) {
        boolean isValid = StatAnalysisUtils.isValidThreshold(category, threshold);

        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidThresholds")
    void isValidThreshold_WithInvalidThresholds(StatCategory category, int threshold) {
        boolean isValid = StatAnalysisUtils.isValidThreshold(category, threshold);

        assertThat(isValid).isFalse();
    }

    // Helper methods
    private static Stream<Arguments> provideValidThresholds() {
        return Stream.of(
                Arguments.of(StatCategory.POINTS, 20),
                Arguments.of(StatCategory.ASSISTS, 6),
                Arguments.of(StatCategory.REBOUNDS, 8)
        );
    }

    private static Stream<Arguments> provideInvalidThresholds() {
        return Stream.of(
                Arguments.of(StatCategory.POINTS, 12),
                Arguments.of(StatCategory.ASSISTS, 7),
                Arguments.of(StatCategory.REBOUNDS, 9)
        );
    }

    private List<GameStats> createTestGames(int count, int points, int assists, int rebounds) {
        List<GameStats> games = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GameStats game = new GameStats();
            game.setPoints(points);
            game.setAssists(assists);
            game.setRebounds(rebounds);
            games.add(game);
        }
        return games;
    }

    private List<GameStats> createMixedGames() {
        List<GameStats> games = new ArrayList<>();
        // 6 games above threshold
        for (int i = 0; i < 6; i++) {
            GameStats game = new GameStats();
            game.setPoints(22);
            game.setAssists(5);
            game.setRebounds(8);
            games.add(game);
        }
        // 4 games below threshold
        for (int i = 0; i < 4; i++) {
            GameStats game = new GameStats();
            game.setPoints(18);
            game.setAssists(5);
            game.setRebounds(8);
            games.add(game);
        }
        return games;
    }

    
}