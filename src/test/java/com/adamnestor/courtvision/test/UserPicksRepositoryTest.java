package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.UserPicks;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class UserPicksRepositoryTest extends UserBaseTestSetup {

    @Test
    void testFindByUser() {
        // Create test pick
        UserPicks testPick = new UserPicks();
        testPick.setUser(testUser);
        testPick.setPlayer(testPlayer);
        testPick.setGame(testGame);
        testPick.setCategory(StatCategory.POINTS);
        testPick.setThreshold(20);
        testPick.setHitRateAtPick(new BigDecimal("80.0"));
        testPick.setResult(true);
        userPicksRepository.save(testPick);

        List<UserPicks> picks = userPicksRepository.findByUser(testUser);

        assertThat(picks)
                .hasSize(1)
                .first()
                .satisfies(pick -> {
                    assertThat(pick.getUser().getId()).isEqualTo(testUser.getId());
                    assertThat(pick.getCategory()).isEqualTo(StatCategory.POINTS);
                    assertThat(pick.getThreshold()).isEqualTo(20);
                });
    }

    @Test
    void testFindByUserOrderByCreatedAtDesc() {
        // Create first pick
        UserPicks firstPick = new UserPicks();
        firstPick.setUser(testUser);
        firstPick.setPlayer(testPlayer);
        firstPick.setGame(testGame);
        firstPick.setCategory(StatCategory.POINTS);
        firstPick.setThreshold(20);
        firstPick.setHitRateAtPick(new BigDecimal("80.0"));
        firstPick.setResult(true);
        userPicksRepository.save(firstPick);

        // Add delay to ensure different timestamps
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Create second pick
        UserPicks secondPick = new UserPicks();
        secondPick.setUser(testUser);
        secondPick.setPlayer(testPlayer);
        secondPick.setGame(testGame);
        secondPick.setCategory(StatCategory.ASSISTS);
        secondPick.setThreshold(5);
        secondPick.setHitRateAtPick(new BigDecimal("75.0"));
        secondPick.setResult(false);
        userPicksRepository.save(secondPick);

        List<UserPicks> picks = userPicksRepository.findByUserOrderByCreatedAtDesc(testUser);

        assertThat(picks)
                .hasSize(2)
                .extracting(UserPicks::getCreatedAt)
                .isSortedAccordingTo((dt1, dt2) -> dt2.compareTo(dt1));

        assertThat(picks.get(0).getCategory()).isEqualTo(StatCategory.ASSISTS);
        assertThat(picks.get(1).getCategory()).isEqualTo(StatCategory.POINTS);
    }

    @Test
    void testFindByUserNoResults() {
        List<UserPicks> picks = userPicksRepository.findByUser(testUser);
        assertThat(picks).isEmpty();
    }
}