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
                .hasSize(1)
                .first()
                .satisfies(hr -> {
                    assertThat(hr.getPlayer().getId()).isEqualTo(testPlayer.getId());
                    assertThat(hr.getCategory()).isEqualTo(StatCategory.POINTS);
                    assertThat(hr.getTimePeriod()).isEqualTo(TimePeriod.L10);
                    assertThat(hr.getHitRate()).isEqualTo(testHitRates.getHitRate());
                });
    }

    @Test
    void testFindTopHitRates() {
        List<HitRates> hitRates = hitRatesRepository.findTopHitRates(
                new BigDecimal("50.0"),
                TimePeriod.L10
        );

        assertThat(hitRates)
                .isNotEmpty()
                .allSatisfy(hr -> {
                    assertThat(hr.getHitRate()).isGreaterThanOrEqualTo(new BigDecimal("50.0"));
                    assertThat(hr.getTimePeriod()).isEqualTo(TimePeriod.L10);
                });
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
                .allSatisfy(hr -> {
                    assertThat(hr.getHitRate()).isGreaterThanOrEqualTo(new BigDecimal("50.0"));
                    assertThat(hr.getCategory()).isEqualTo(StatCategory.POINTS);
                    assertThat(hr.getTimePeriod()).isEqualTo(TimePeriod.L10);
                });
    }
}