package com.adamnestor.courtvision.test.repository;

import com.adamnestor.courtvision.test.config.BaseTestSetup;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class PlayersRepositoryTest extends BaseTestSetup {

    @Test
    void testFindByExternalId() {
        assertThat(playersRepository.findByExternalId(testPlayer.getExternalId()))
                .isPresent()
                .hasValueSatisfying(player -> {
                    assertThat(player.getFirstName()).isEqualTo(testPlayer.getFirstName());
                    assertThat(player.getLastName()).isEqualTo(testPlayer.getLastName());
                    assertThat(player.getExternalId()).isEqualTo(testPlayer.getExternalId());
                    assertThat(player.getTeam().getId()).isEqualTo(testTeam.getId());
                });
    }
}