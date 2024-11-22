// GameStatsRepositoryTest.java
package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class GameStatsRepositoryTest {

    @Autowired
    private GameStatsRepository gameStatsRepository;

    @Autowired
    private PlayersRepository playersRepository;

    @Autowired
    private GamesRepository gamesRepository;

    private Players testPlayer;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        testPlayer = playersRepository.findById(1L).orElseThrow();
        startDate = LocalDate.now().minusDays(30);
        endDate = LocalDate.now();
    }

    @Test
    void testFindByPlayerAndPointsThreshold() {
        Integer threshold = 20;
        List<GameStats> stats = gameStatsRepository.findByPlayerAndPointsThreshold(testPlayer, threshold);

        assertThat(stats).isNotEmpty();
        stats.forEach(stat -> {
            assertThat(stat.getPoints()).isGreaterThanOrEqualTo(threshold);
            assertThat(stat.getPlayer()).isEqualTo(testPlayer);
        });
    }

    @Test
    void testCalculateAveragePointsInDateRange() {
        Double average = gameStatsRepository.calculateAveragePointsInDateRange(testPlayer, startDate, endDate);

        assertThat(average).isNotNull();
        assertThat(average).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void testCalculateHitRateForPoints() {
        Integer threshold = 20;
        Double hitRate = gameStatsRepository.calculateHitRateForPoints(testPlayer, threshold, startDate, endDate);

        assertThat(hitRate).isNotNull();
        assertThat(hitRate).isBetween(0.0, 100.0);
    }

    @Test
    void testCalculateHitRateForAssists() {
        Integer threshold = 5;
        Double hitRate = gameStatsRepository.calculateHitRateForAssists(testPlayer, threshold, startDate, endDate);

        assertThat(hitRate).isNotNull();
        assertThat(hitRate).isBetween(0.0, 100.0);
    }

    @Test
    void testCalculateHitRateForRebounds() {
        Integer threshold = 5;
        Double hitRate = gameStatsRepository.calculateHitRateForRebounds(testPlayer, threshold, startDate, endDate);

        assertThat(hitRate).isNotNull();
        assertThat(hitRate).isBetween(0.0, 100.0);
    }
}