// PlayersRepositoryTest.java
package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.repository.TeamsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class PlayersRepositoryTest {

    @Autowired
    private PlayersRepository playersRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    @Test
    void testFindActivePlayersByConference() {
        String conference = "East";
        List<Players> players = playersRepository.findActivePlayersByConference(conference);

        assertThat(players).isNotEmpty();
        players.forEach(player -> {
            assertThat(player.getStatus()).isEqualTo(PlayerStatus.ACTIVE);
            assertThat(player.getTeam().getConference().toString()).isEqualTo(conference);
        });
    }

    @Test
    void testFindActivePlayersByTeam() {
        Teams team = teamsRepository.findById(1L).orElseThrow();
        List<Players> players = playersRepository.findActivePlayersByTeam(team);

        assertThat(players).isNotEmpty();
        players.forEach(player -> {
            assertThat(player.getStatus()).isEqualTo(PlayerStatus.ACTIVE);
            assertThat(player.getTeam()).isEqualTo(team);
        });
    }
}