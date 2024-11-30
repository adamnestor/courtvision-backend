package com.adamnestor.courtvision.test.repository;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.test.config.BaseTestSetup;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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

    @Test
    void findByStatus_WhenActive_ReturnsOnlyActivePlayers() {
        // Create another active player to ensure we get multiple results
        Players anotherActive = createTestPlayer("Another", "Active", PlayerStatus.ACTIVE, 2L);
        playersRepository.save(anotherActive);

        List<Players> activePlayers = playersRepository.findByStatus(PlayerStatus.ACTIVE);

        assertThat(activePlayers)
                .hasSize(2)  // testPlayer from BaseTestSetup + our new player
                .extracting(Players::getStatus)
                .containsOnly(PlayerStatus.ACTIVE);
    }

    @Test
    void findByStatus_WhenInactive_ReturnsOnlyInactivePlayers() {
        Players inactivePlayer = createTestPlayer("Former", "Player", PlayerStatus.INACTIVE, 3L);
        playersRepository.save(inactivePlayer);

        List<Players> inactivePlayers = playersRepository.findByStatus(PlayerStatus.INACTIVE);

        assertThat(inactivePlayers)
                .hasSize(1)
                .extracting(Players::getStatus)
                .containsOnly(PlayerStatus.INACTIVE);
    }

    @Test
    void findByStatus_WhenInjured_ReturnsEmptyList() {
        List<Players> injuredPlayers = playersRepository.findByStatus(PlayerStatus.INJURED);
        assertThat(injuredPlayers).isEmpty();
    }

    private Players createTestPlayer(String firstName, String lastName, PlayerStatus status, Long externalId) {
        Players player = new Players();
        player.setFirstName(firstName);
        player.setLastName(lastName);
        player.setTeam(testTeam);
        player.setStatus(status);
        player.setExternalId(externalId);  // Added this line
        return player;
    }
}