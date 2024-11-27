package com.adamnestor.courtvision.test.repository;

import com.adamnestor.courtvision.domain.GameStatus;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.test.config.BaseTestSetup;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.time.LocalDate;

public class GamesRepositoryTest extends BaseTestSetup {

    @Test
    void testFindByExternalId() {
        assertThat(gamesRepository.findByExternalId(1L))
                .isPresent()
                .hasValueSatisfying(game -> {
                    assertThat(game.getExternalId()).isEqualTo(testGame.getExternalId());
                    assertThat(game.getGameDate()).isEqualTo(testGame.getGameDate());
                    assertThat(game.getStatus()).isEqualTo(testGame.getStatus());
                    assertThat(game.getHomeTeam().getId()).isEqualTo(testTeam.getId());
                    assertThat(game.getAwayTeam().getId()).isEqualTo(testTeam.getId());
                });
    }

    @Test
    void testFindByGameDateBetweenAndStatus() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(1);

        List<Games> games = gamesRepository.findByGameDateBetweenAndStatus(
                start, end, GameStatus.FINAL);

        assertThat(games)
                .isNotEmpty()
                .hasSize(1)
                .first()
                .satisfies(game -> {
                    assertThat(game.getGameDate()).isEqualTo(testGame.getGameDate());
                    assertThat(game.getStatus()).isEqualTo(GameStatus.FINAL);
                    assertThat(game.getExternalId()).isEqualTo(testGame.getExternalId());
                });
    }

    @Test
    void testFindBySeasonAndStatus() {
        List<Games> games = gamesRepository.findBySeasonAndStatus(2024, GameStatus.FINAL);

        assertThat(games)
                .isNotEmpty()
                .hasSize(1)
                .first()
                .satisfies(game -> {
                    assertThat(game.getSeason()).isEqualTo(2024);
                    assertThat(game.getStatus()).isEqualTo(GameStatus.FINAL);
                    assertThat(game.getExternalId()).isEqualTo(testGame.getExternalId());
                });
    }

    @Test
    void testFindUpcomingGames() {
        // Create a future game first
        Games futureGame = new Games();
        futureGame.setGameDate(LocalDate.now().plusDays(5));
        futureGame.setHomeTeam(testTeam);
        futureGame.setAwayTeam(testTeam);
        futureGame.setStatus(GameStatus.SCHEDULED);
        futureGame.setSeason(2024);
        futureGame.setExternalId(2L);
        gamesRepository.save(futureGame);

        List<Games> upcomingGames = gamesRepository.findUpcomingGames(LocalDate.now());

        assertThat(upcomingGames)
                .isNotEmpty()
                .allSatisfy(game -> {
                    assertThat(game.getGameDate()).isAfterOrEqualTo(LocalDate.now());
                    assertThat(game.getStatus()).isEqualTo(GameStatus.SCHEDULED);
                });
    }

    @Test
    void testFindTeamSchedule() {
        // Add a future game for the team
        Games futureGame = new Games();
        futureGame.setGameDate(LocalDate.now().plusDays(5));
        futureGame.setHomeTeam(testTeam);
        futureGame.setAwayTeam(testTeam);
        futureGame.setStatus(GameStatus.SCHEDULED);
        futureGame.setSeason(2024);
        futureGame.setExternalId(2L);
        gamesRepository.save(futureGame);

        List<Games> schedule = gamesRepository.findTeamSchedule(testTeam, LocalDate.now());

        assertThat(schedule)
                .isNotEmpty()
                .allSatisfy(game -> {
                    assertThat(game.getGameDate()).isAfterOrEqualTo(LocalDate.now());
                    assertThat(game.getHomeTeam().getId().equals(testTeam.getId()) ||
                            game.getAwayTeam().getId().equals(testTeam.getId())).isTrue();
                });
    }
}