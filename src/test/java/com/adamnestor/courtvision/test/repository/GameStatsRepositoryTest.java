package com.adamnestor.courtvision.test.repository;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.GameStatus;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.test.config.BaseTestSetup;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class GameStatsRepositoryTest extends BaseTestSetup {

    private final AtomicLong gameIdCounter = new AtomicLong(1000);  // Starting counter for unique IDs

    @Test
    void testFindPlayerRecentGames() {
        var games = gameStatsRepository.findPlayerRecentGames(testPlayer);

        assertThat(games)
                .isNotEmpty()
                .hasSize(1)  // We expect 1 game from our test data
                .first()
                .satisfies(stats -> {
                    assertThat(stats.getPlayer().getId()).isEqualTo(testPlayer.getId());
                    assertThat(stats.getPoints()).isEqualTo(testGameStats.getPoints());
                    assertThat(stats.getGame().getGameDate()).isEqualTo(testGame.getGameDate());
                });
    }

    @Test
    void testFindPlayerRecentGamesOrdering() {
        // Create additional test games with different dates
        GameStats olderGame = createGameStats(testPlayer, LocalDateTime.now().minusDays(5), 15);
        GameStats newerGame = createGameStats(testPlayer, LocalDateTime.now(), 20);
        gameStatsRepository.saveAll(List.of(olderGame, newerGame));

        var games = gameStatsRepository.findPlayerRecentGames(testPlayer);

        assertThat(games)
                .isNotEmpty()
                .hasSize(3)  // Original test game + 2 new ones
                .extracting(stats -> stats.getGame().getGameDate())
                .isSortedAccordingTo((date1, date2) -> date2.compareTo(date1));  // Descending order
    }

    @Test
    void testFindPlayerRecentGames_NoGames() {
        // Create a player with no games
        Players playerWithNoGames = new Players();
        playerWithNoGames.setExternalId(999L);
        playerWithNoGames.setFirstName("No");
        playerWithNoGames.setLastName("Games");
        playerWithNoGames.setTeam(testTeam);
        playersRepository.save(playerWithNoGames);

        var games = gameStatsRepository.findPlayerRecentGames(playerWithNoGames);

        assertThat(games).isEmpty();
    }

    @Test
    void testFindPlayerRecentGames_MultipleGamesInOneDay() {
        LocalDateTime today = LocalDateTime.now();
        GameStats game1 = createGameStats(testPlayer, today, 25);
        GameStats game2 = createGameStats(testPlayer, today, 30);
        gameStatsRepository.saveAll(List.of(game1, game2));

        var games = gameStatsRepository.findPlayerRecentGames(testPlayer);

        assertThat(games)
                .hasSize(3)  // Original test game + 2 new ones
                .extracting(GameStats::getPoints)
                .contains(25, 30);
    }

    private GameStats createGameStats(Players player, LocalDateTime date, int points) {
        Games game = new Games();
        game.setGameDate(date);
        game.setStatus(GameStatus.FINAL);
        game.setHomeTeam(testTeam);
        game.setAwayTeam(testTeam);
        game.setSeason(2024);
        game.setExternalId(gameIdCounter.getAndIncrement());  // Generate unique ID
        gamesRepository.save(game);

        GameStats stats = new GameStats();
        stats.setPlayer(player);
        stats.setGame(game);
        stats.setPoints(points);
        stats.setAssists(5);
        stats.setRebounds(5);
        return stats;
    }
}