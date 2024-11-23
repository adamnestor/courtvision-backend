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
}