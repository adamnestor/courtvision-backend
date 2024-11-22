package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.domain.Players;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

public class PlayersRepositoryTest extends BaseTestSetup {

    @Test
    void testFindByTeam() {
        List<Players> players = playersRepository.findByTeam(testTeam);
        assertThat(players).isNotEmpty();
        assertThat(players.get(0)).isEqualTo(testPlayer);
    }

    @Test
    void testFindByStatus() {
        List<Players> players = playersRepository.findByStatus(PlayerStatus.ACTIVE);
        assertThat(players).isNotEmpty();
        assertThat(players.get(0).getStatus()).isEqualTo(PlayerStatus.ACTIVE);
    }

    @Test
    void testFindByExternalId() {
        assertThat(playersRepository.findByExternalId(1L))
                .isPresent()
                .get()
                .isEqualTo(testPlayer);
    }
}