package com.adamnestor.courtvision.test;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.adamnestor.courtvision.domain.GameStats;
import java.util.List;

public class GameStatsRepositoryTest extends BaseTestSetup {

    @Test
    void testFindByPlayerAndGame() {
        assertThat(gameStatsRepository.findByPlayerAndGame(testPlayer, testGame))
                .isPresent()
                .get()
                .isEqualTo(testGameStats);
    }

    @Test
    void testFindByPlayerAndGameGameDateBetween() {
        List<GameStats> stats = gameStatsRepository.findByPlayerAndGameGameDateBetween(
                testPlayer,
                testGame.getGameDate().minusDays(1),
                testGame.getGameDate().plusDays(1)
        );

        assertThat(stats)
                .isNotEmpty()
                .contains(testGameStats);
    }

    @Test
    void testFindByPlayerAndPointsThreshold() {
        List<GameStats> stats = gameStatsRepository.findByPlayerAndPointsThreshold(testPlayer, 15);

        assertThat(stats)
                .isNotEmpty()
                .allMatch(stat -> stat.getPoints() >= 15);
    }

    @Test
    void testCalculateAveragePointsInDateRange() {
        Double average = gameStatsRepository.calculateAveragePointsInDateRange(
                testPlayer,
                testGame.getGameDate().minusDays(1),
                testGame.getGameDate().plusDays(1)
        );

        assertThat(average).isEqualTo(20.0);
    }

    @Test
    void testCalculateHitRateForPoints() {
        Double hitRate = gameStatsRepository.calculateHitRateForPoints(
                testPlayer,
                15,
                testGame.getGameDate().minusDays(1),
                testGame.getGameDate().plusDays(1)
        );

        assertThat(hitRate).isNotNull();
        assertThat(hitRate).isBetween(0.0, 100.0);
    }
}