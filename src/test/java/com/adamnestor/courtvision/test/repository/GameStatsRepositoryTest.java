package com.adamnestor.courtvision.test.repository;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.GameStatus;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.test.config.BaseTestSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class GameStatsRepositoryTest extends BaseTestSetup {

    @Autowired
    private GameStatsRepository gameStatsRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testFindPlayerRecentGames() {
        // Get games for our test player
        var games = gameStatsRepository.findPlayerRecentGames(testPlayer);

        assertThat(games)
                .isNotEmpty()
                .hasSize(1)  // We expect 1 game from our test data
                .first()
                .satisfies(stats -> {
                    assertThat(stats.getPlayer().getId()).isEqualTo(testPlayer.getId());
                    assertThat(stats.getPoints()).isEqualTo(testGameStats.getPoints());
                    // Optional: verify it's ordered by date DESC
                    assertThat(stats.getGame().getGameDate()).isEqualTo(testGame.getGameDate());
                });
    }

    @Test
    void testFindPlayerRecentGamesOrdering() {
        // Create and save additional game stats with different dates
        GameStats olderStats = createGameStats(testPlayer, LocalDate.now().minusDays(5));
        GameStats newerStats = createGameStats(testPlayer, LocalDate.now());
        gameStatsRepository.saveAll(List.of(olderStats, newerStats));

        // Get the games
        var games = gameStatsRepository.findPlayerRecentGames(testPlayer);

        // Verify we get games in descending date order
        assertThat(games)
                .isNotEmpty()
                .hasSize(3)  // Original test game + 2 new ones
                .isSortedAccordingTo((g1, g2) ->
                        g2.getGame().getGameDate().compareTo(g1.getGame().getGameDate()));
    }

    private GameStats createGameStats(Players player, LocalDate date) {
        Games game = new Games();
        game.setGameDate(date);
        game.setStatus(GameStatus.FINAL);
        game.setHomeTeam(player.getTeam());
        game.setAwayTeam(player.getTeam());
        game.setSeason(2024);
        game.setExternalId(date.toEpochDay());
        gamesRepository.save(game);

        GameStats stats = new GameStats();
        stats.setPlayer(player);
        stats.setGame(game);
        stats.setPoints(10);
        stats.setAssists(5);
        stats.setRebounds(5);
        return stats;
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
    void testPlayerWithNoGames() {
        // Create a new player with no games
        Players playerWithNoGames = new Players();
        playerWithNoGames.setExternalId(999L);
        playerWithNoGames.setFirstName("Test");
        playerWithNoGames.setLastName("Player");
        playersRepository.save(playerWithNoGames);

        var games = gameStatsRepository.findPlayerRecentGames(playerWithNoGames);

        assertThat(games).isEmpty();
    }
}