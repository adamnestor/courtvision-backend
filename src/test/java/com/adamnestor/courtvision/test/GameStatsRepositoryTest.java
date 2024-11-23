package com.adamnestor.courtvision.test;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

public class GameStatsRepositoryTest extends BaseTestSetup {

    @Test
    void testFindPlayerRecentGames() {
        LocalDate startDate = testGame.getGameDate().minusDays(5);
        LocalDate endDate = testGame.getGameDate();

        var games = gameStatsRepository.findPlayerRecentGames(testPlayer, startDate, endDate);

        assertThat(games)
                .isNotEmpty()
                .hasSize(1)
                .first()
                .satisfies(stats -> {
                    assertThat(stats.getPlayer().getId()).isEqualTo(testPlayer.getId());
                    assertThat(stats.getGame().getGameDate()).isBetween(startDate, endDate);
                    assertThat(stats.getPoints()).isEqualTo(testGameStats.getPoints());
                });
    }

    @Test
    void testCalculatePointsHitRate() {
        LocalDate startDate = testGame.getGameDate().minusDays(5);
        LocalDate endDate = testGame.getGameDate();

        Double hitRate = gameStatsRepository.calculatePointsHitRate(
                testPlayer,
                15, // threshold
                startDate,
                endDate
        );

        assertThat(hitRate).isNotNull();
        assertThat(hitRate).isBetween(0.0, 100.0);
    }

    @Test
    void testCalculateAssistsHitRate() {
        LocalDate startDate = testGame.getGameDate().minusDays(5);
        LocalDate endDate = testGame.getGameDate();

        Double hitRate = gameStatsRepository.calculateAssistsHitRate(
                testPlayer,
                5, // threshold
                startDate,
                endDate
        );

        assertThat(hitRate).isNotNull();
        assertThat(hitRate).isBetween(0.0, 100.0);
    }

    @Test
    void testCalculateReboundsHitRate() {
        LocalDate startDate = testGame.getGameDate().minusDays(5);
        LocalDate endDate = testGame.getGameDate();

        Double hitRate = gameStatsRepository.calculateReboundsHitRate(
                testPlayer,
                5, // threshold
                startDate,
                endDate
        );

        assertThat(hitRate).isNotNull();
        assertThat(hitRate).isBetween(0.0, 100.0);
    }

    @Test
    void testCalculatePointsAverage() {
        LocalDate startDate = testGame.getGameDate().minusDays(5);
        LocalDate endDate = testGame.getGameDate();

        Double average = gameStatsRepository.calculatePointsAverage(
                testPlayer,
                startDate,
                endDate
        );

        assertThat(average).isNotNull();
        assertThat(average).isEqualTo(testGameStats.getPoints().doubleValue());
    }

    @Test
    void testNoStatsInDateRange() {
        LocalDate futureStart = testGame.getGameDate().plusDays(1);
        LocalDate futureEnd = testGame.getGameDate().plusDays(5);

        var games = gameStatsRepository.findPlayerRecentGames(
                testPlayer,
                futureStart,
                futureEnd
        );

        assertThat(games).isEmpty();
    }
}