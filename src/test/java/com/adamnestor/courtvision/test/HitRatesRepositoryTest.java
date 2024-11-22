// HitRatesRepositoryTest.java
package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.HitRates;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.repository.HitRatesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class HitRatesRepositoryTest {

    @Autowired
    private HitRatesRepository hitRatesRepository;

    @Test
    void testFindTopHitRates() {
        List<HitRates> hitRates = hitRatesRepository.findTopHitRates(
                new BigDecimal("50.0"),
                TimePeriod.L10
        );
        assertThat(hitRates).isNotEmpty();
        assertThat(hitRates).isSortedAccordingTo(
                (h1, h2) -> h2.getHitRate().compareTo(h1.getHitRate())
        );
    }

    @Test
    void testFindTopHitRatesByCategory() {
        List<HitRates> hitRates = hitRatesRepository.findTopHitRatesByCategory(
                StatCategory.POINTS,
                TimePeriod.L10,
                new BigDecimal("50.0")
        );
        assertThat(hitRates).isNotEmpty();
        hitRates.forEach(hitRate -> {
            assertThat(hitRate.getCategory()).isEqualTo(StatCategory.POINTS);
            assertThat(hitRate.getTimePeriod()).isEqualTo(TimePeriod.L10);
            assertThat(hitRate.getHitRate()).isGreaterThanOrEqualTo(new BigDecimal("50.0"));
        });
        assertThat(hitRates).isSortedAccordingTo(
                (h1, h2) -> h2.getHitRate().compareTo(h1.getHitRate())
        );
    }
}