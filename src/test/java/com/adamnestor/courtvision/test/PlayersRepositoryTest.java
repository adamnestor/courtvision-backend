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

        assertThat(players)
                .isNotEmpty()
                .hasSize(1)
                .first()
                .satisfies(player -> {
                    assertThat(player.getFirstName()).isEqualTo(testPlayer.getFirstName());
                    assertThat(player.getLastName()).isEqualTo(testPlayer.getLastName());
                    assertThat(player.getExternalId()).isEqualTo(testPlayer.getExternalId());
                    assertThat(player.getTeam().getId()).isEqualTo(testTeam.getId());
                });
    }

    @Test
    void testFindByStatus() {
        List<Players> players = playersRepository.findByStatus(PlayerStatus.ACTIVE);

        assertThat(players)
                .isNotEmpty()
                .hasSize(1)
                .first()
                .satisfies(player -> {
                    assertThat(player.getStatus()).isEqualTo(PlayerStatus.ACTIVE);
                    assertThat(player.getFirstName()).isEqualTo(testPlayer.getFirstName());
                    assertThat(player.getLastName()).isEqualTo(testPlayer.getLastName());
                });
    }

    @Test
    void testFindByExternalId() {
        assertThat(playersRepository.findByExternalId(1L))
                .isPresent()
                .hasValueSatisfying(player -> {
                    assertThat(player.getFirstName()).isEqualTo(testPlayer.getFirstName());
                    assertThat(player.getLastName()).isEqualTo(testPlayer.getLastName());
                    assertThat(player.getExternalId()).isEqualTo(testPlayer.getExternalId());
                    assertThat(player.getTeam().getId()).isEqualTo(testTeam.getId());
                });
    }
}