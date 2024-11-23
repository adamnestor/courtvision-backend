package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class HitRatesRepositoryTest extends BaseTestSetup {

    @Test
    void testFindTopHitRates() {
        var hitRates = hitRatesRepository.findTopHitRates(
                TimePeriod.L10,
                StatCategory.POINTS,
                20
        );

        assertThat(hitRates)
                .isNotEmpty()
                .allSatisfy(hr -> {
                    assertThat(hr.getTimePeriod()).isEqualTo(TimePeriod.L10);
                    assertThat(hr.getCategory()).isEqualTo(StatCategory.POINTS);
                    assertThat(hr.getThreshold()).isEqualTo(20);
                })
                .isSortedAccordingTo((hr1, hr2) -> hr2.getHitRate().compareTo(hr1.getHitRate()));
    }

    @Test
    void testFindByPlayerAndCategoryAndTimePeriodOrderByHitRateDesc() {
        var hitRates = hitRatesRepository.findByPlayerAndCategoryAndTimePeriodOrderByHitRateDesc(
                testPlayer,
                StatCategory.POINTS,
                TimePeriod.L10
        );

        assertThat(hitRates)
                .isNotEmpty()
                .allSatisfy(hr -> {
                    assertThat(hr.getPlayer().getId()).isEqualTo(testPlayer.getId());
                    assertThat(hr.getCategory()).isEqualTo(StatCategory.POINTS);
                    assertThat(hr.getTimePeriod()).isEqualTo(TimePeriod.L10);
                })
                .isSortedAccordingTo((hr1, hr2) -> hr2.getHitRate().compareTo(hr1.getHitRate()));
    }
}