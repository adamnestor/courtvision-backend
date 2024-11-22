// GamesRepositoryTest.java
package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.domain.GameStatus;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.TeamsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class GamesRepositoryTest {

    @Autowired
    private GamesRepository gamesRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    @Test
    void testFindUpcomingGames() {
        List<Games> games = gamesRepository.findUpcomingGames(LocalDate.now());
        assertThat(games).isNotEmpty();
        games.forEach(game -> {
            assertThat(game.getGameDate()).isAfterOrEqualTo(LocalDate.now());
            assertThat(game.getStatus()).isEqualTo(GameStatus.SCHEDULED);
        });
    }

    @Test
    void testFindTeamSchedule() {
        Teams team = teamsRepository.findById(1L).orElseThrow();
        List<Games> games = gamesRepository.findTeamSchedule(team, LocalDate.now());
        assertThat(games).isNotEmpty();
        games.forEach(game -> {
            assertThat(game.getGameDate()).isAfterOrEqualTo(LocalDate.now());
            assertThat(game.getHomeTeam().equals(team) || game.getAwayTeam().equals(team)).isTrue();
        });
    }
}