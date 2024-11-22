package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.HitRates;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.math.BigDecimal;

public class HitRatesRepositoryTest extends BaseTestSetup {

    @Test
    void testFindByPlayerAndCategoryAndTimePeriod() {
        List<HitRates> hitRates = hitRatesRepository.findByPlayerAndCategoryAndTimePeriod(
                testPlayer,
                StatCategory.POINTS,
                TimePeriod.L10
        );

        assertThat(hitRates)
                .isNotEmpty()
                .contains(testHitRates);
    }

    @Test
    void testFindByPlayerAndTimePeriod() {
        List<HitRates> hitRates = hitRatesRepository.findByPlayerAndTimePeriod(
                testPlayer,
                TimePeriod.L10
        );

        assertThat(hitRates)
                .isNotEmpty()
                .contains(testHitRates);
    }

    @Test
    void testFindTopHitRates() {
        List<HitRates> hitRates = hitRatesRepository.findTopHitRates(
                new BigDecimal("50.0"),
                TimePeriod.L10
        );

        assertThat(hitRates)
                .isNotEmpty()
                .allMatch(hr -> hr.getHitRate().compareTo(new BigDecimal("50.0")) >= 0);
    }

    @Test
    void testFindTopHitRatesByCategory() {
        List<HitRates> hitRates = hitRatesRepository.findTopHitRatesByCategory(
                StatCategory.POINTS,
                TimePeriod.L10,
                new BigDecimal("50.0")
        );

        assertThat(hitRates)
                .isNotEmpty()
                .allMatch(hr -> hr.getHitRate().compareTo(new BigDecimal("50.0")) >= 0)
                .allMatch(hr -> hr.getCategory() == StatCategory.POINTS);
    }
}