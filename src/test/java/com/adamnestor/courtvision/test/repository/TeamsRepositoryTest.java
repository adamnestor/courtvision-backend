package com.adamnestor.courtvision.test.repository;

import com.adamnestor.courtvision.test.config.BaseTestSetup;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TeamsRepositoryTest extends BaseTestSetup {

    @Test
    void testFindByExternalId() {
        assertThat(teamsRepository.findByExternalId(testTeam.getExternalId()))
                .isPresent()
                .hasValueSatisfying(team -> {
                    assertThat(team.getName()).isEqualTo(testTeam.getName());
                    assertThat(team.getCity()).isEqualTo(testTeam.getCity());
                    assertThat(team.getAbbreviation()).isEqualTo(testTeam.getAbbreviation());
                    assertThat(team.getExternalId()).isEqualTo(testTeam.getExternalId());
                });
    }

    @Test
    void testFindByAbbreviation() {
        assertThat(teamsRepository.findByAbbreviation(testTeam.getAbbreviation()))
                .isPresent()
                .hasValueSatisfying(team -> {
                    assertThat(team.getName()).isEqualTo(testTeam.getName());
                    assertThat(team.getCity()).isEqualTo(testTeam.getCity());
                    assertThat(team.getExternalId()).isEqualTo(testTeam.getExternalId());
                    assertThat(team.getAbbreviation()).isEqualTo(testTeam.getAbbreviation());
                });
    }
}