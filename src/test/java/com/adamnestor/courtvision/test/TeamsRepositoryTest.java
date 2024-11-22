package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.Conference;
import com.adamnestor.courtvision.domain.Teams;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;

public class TeamsRepositoryTest extends BaseTestSetup {

    @Test
    void testFindByExternalId() {
        Optional<Teams> team = teamsRepository.findByExternalId(1L);

        assertThat(team)
                .isPresent()
                .hasValueSatisfying(t -> {
                    assertThat(t.getName()).isEqualTo(testTeam.getName());
                    assertThat(t.getCity()).isEqualTo(testTeam.getCity());
                    assertThat(t.getAbbreviation()).isEqualTo(testTeam.getAbbreviation());
                    assertThat(t.getExternalId()).isEqualTo(testTeam.getExternalId());
                    assertThat(t.getConference()).isEqualTo(testTeam.getConference());
                    assertThat(t.getDivision()).isEqualTo(testTeam.getDivision());
                });
    }

    @Test
    void testFindByAbbreviation() {
        Optional<Teams> team = teamsRepository.findByAbbreviation("TST");

        assertThat(team)
                .isPresent()
                .hasValueSatisfying(t -> {
                    assertThat(t.getName()).isEqualTo("Test Team");
                    assertThat(t.getCity()).isEqualTo("Test City");
                    assertThat(t.getExternalId()).isEqualTo(1L);
                });
    }

    @Test
    void testSaveTeam() {
        Teams newTeam = new Teams();
        newTeam.setName("New Team");
        newTeam.setCity("New City");
        newTeam.setAbbreviation("NEW");
        newTeam.setConference(Conference.West);
        newTeam.setDivision("New Division");
        newTeam.setExternalId(2L);

        Teams savedTeam = teamsRepository.save(newTeam);

        assertThat(savedTeam.getId()).isNotNull();
        assertThat(savedTeam.getName()).isEqualTo("New Team");
        assertThat(savedTeam.getAbbreviation()).isEqualTo("NEW");
        assertThat(savedTeam.getExternalId()).isEqualTo(2L);
    }
}