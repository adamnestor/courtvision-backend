package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.GameStatus;
import com.adamnestor.courtvision.domain.Games;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.time.LocalDate;

public class GamesRepositoryTest extends BaseTestSetup {

    @Test
    void testFindByExternalId() {
        assertThat(gamesRepository.findByExternalId(1L))
                .isPresent()
                .get()
                .isEqualTo(testGame);
    }

    @Test
    void testFindByGameDateBetweenAndStatus() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(1);

        List<Games> games = gamesRepository.findByGameDateBetweenAndStatus(
                start, end, GameStatus.FINAL);

        assertThat(games).isNotEmpty();
        assertThat(games.get(0)).isEqualTo(testGame);
    }

    @Test
    void testFindBySeasonAndStatus() {
        List<Games> games = gamesRepository.findBySeasonAndStatus(2024, GameStatus.FINAL);

        assertThat(games).isNotEmpty();
        assertThat(games.get(0)).isEqualTo(testGame);
    }

    @Test
    void testFindUpcomingGames() {
        List<Games> games = gamesRepository.findUpcomingGames(LocalDate.now());

        assertThat(games)
                .allMatch(game -> game.getGameDate().isAfter(LocalDate.now().minusDays(1)))
                .allMatch(game -> game.getStatus().equals(GameStatus.SCHEDULED));
    }

    @Test
    void testFindTeamSchedule() {
        List<Games> games = gamesRepository.findTeamSchedule(testTeam, LocalDate.now());

        assertThat(games)
                .isNotEmpty()
                .allMatch(game ->
                        game.getHomeTeam().equals(testTeam) ||
                                game.getAwayTeam().equals(testTeam));
    }
}