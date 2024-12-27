package com.adamnestor.courtvision.test.repository;

import com.adamnestor.courtvision.domain.HitRates;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.test.config.BaseTestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HitRatesRepositoryTest extends BaseTestSetup {

    @Test
    void testFindTopHitRates() {
        // Create test hit rates
        createHitRate(testPlayer, StatCategory.POINTS, 20, TimePeriod.L10, new BigDecimal("80.0"));
        createHitRate(testPlayer, StatCategory.POINTS, 20, TimePeriod.L10, new BigDecimal("70.0"));

        var hitRates = hitRatesRepository.findTopHitRates(TimePeriod.L10, StatCategory.POINTS, 20);

        assertThat(hitRates)
                .isNotEmpty()
                .isSortedAccordingTo((hr1, hr2) -> hr2.getHitRate().compareTo(hr1.getHitRate()))
                .allSatisfy(hr -> {
                    assertThat(hr.getTimePeriod()).isEqualTo(TimePeriod.L10);
                    assertThat(hr.getCategory()).isEqualTo(StatCategory.POINTS);
                    assertThat(hr.getThreshold()).isEqualTo(20);
                });
    }

    @ParameterizedTest
    @EnumSource(StatCategory.class)
    void testFindTopHitRates_AllCategories(StatCategory category) {
        createHitRate(testPlayer, category, getDefaultThreshold(category), TimePeriod.L10, new BigDecimal("75.0"));

        var hitRates = hitRatesRepository.findTopHitRates(
                TimePeriod.L10,
                category,
                getDefaultThreshold(category)
        );

        assertThat(hitRates)
                .isNotEmpty()
                .allSatisfy(hr -> assertThat(hr.getCategory()).isEqualTo(category));
    }

    @ParameterizedTest
    @EnumSource(TimePeriod.class)
    void testFindTopHitRates_AllTimePeriods(TimePeriod period) {
        createHitRate(testPlayer, StatCategory.POINTS, 20, period, new BigDecimal("75.0"));

        var hitRates = hitRatesRepository.findTopHitRates(period, StatCategory.POINTS, 20);

        assertThat(hitRates)
                .isNotEmpty()
                .allSatisfy(hr -> assertThat(hr.getTimePeriod()).isEqualTo(period));
    }

    @Test
    void testFindTopHitRates_NoResults() {
        var hitRates = hitRatesRepository.findTopHitRates(
                TimePeriod.L10,
                StatCategory.POINTS,
                99  // Non-existent threshold
        );

        assertThat(hitRates).isEmpty();
    }

    private HitRates createHitRate(Players player, StatCategory category, Integer threshold,
                                   TimePeriod timePeriod, BigDecimal hitRate) {
        HitRates hr = new HitRates();
        hr.setPlayer(player);
        hr.setCategory(category);
        hr.setThreshold(threshold);
        hr.setTimePeriod(timePeriod);
        hr.setHitRate(hitRate);
        hr.setAverage(new BigDecimal("25.0"));
        hr.setGamesCounted(10);
        hr.setLastCalculated(LocalDateTime.now());
        return hitRatesRepository.save(hr);
    }

    private int getDefaultThreshold(StatCategory category) {
        return switch (category) {
            case POINTS -> 20;
            case ASSISTS -> 4;
            case REBOUNDS -> 8;
            case ALL -> throw new IllegalArgumentException("Cannot get default threshold for ALL category");
        };
    }
}