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
                .hasValueSatisfying(stats -> {
                    assertThat(stats.getPoints()).isEqualTo(testGameStats.getPoints());
                    assertThat(stats.getAssists()).isEqualTo(testGameStats.getAssists());
                    assertThat(stats.getRebounds()).isEqualTo(testGameStats.getRebounds());
                    assertThat(stats.getPlayer().getId()).isEqualTo(testPlayer.getId());
                    assertThat(stats.getGame().getId()).isEqualTo(testGame.getId());
                });
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
                .hasSize(1)
                .first()
                .satisfies(stat -> {
                    assertThat(stat.getPoints()).isEqualTo(testGameStats.getPoints());
                    assertThat(stat.getPlayer().getId()).isEqualTo(testPlayer.getId());
                    assertThat(stat.getGame().getId()).isEqualTo(testGame.getId());
                });
    }

    @Test
    void testFindByPlayerAndPointsThreshold() {
        List<GameStats> stats = gameStatsRepository.findByPlayerAndPointsThreshold(testPlayer, 15);

        assertThat(stats)
                .isNotEmpty()
                .hasSize(1)
                .first()
                .satisfies(stat -> {
                    assertThat(stat.getPoints()).isGreaterThanOrEqualTo(15);
                    assertThat(stat.getPlayer().getId()).isEqualTo(testPlayer.getId());
                });
    }
}