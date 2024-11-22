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
                .get()
                .isEqualTo(testTeam);
    }

    @Test
    void testFindByAbbreviation() {
        Optional<Teams> team = teamsRepository.findByAbbreviation("TST");

        assertThat(team)
                .isPresent()
                .get()
                .hasFieldOrPropertyWithValue("name", "Test Team")
                .hasFieldOrPropertyWithValue("city", "Test City");
    }

    @Test
    void testSaveTeam() {
        Teams newTeam = new Teams();
        newTeam.setName("New Team");
        newTeam.setCity("New City");
        newTeam.setAbbreviation("NEW");  // Different from TST
        newTeam.setConference(Conference.West);
        newTeam.setDivision("New Division");
        newTeam.setExternalId(2L);       // Different from 1L

        Teams savedTeam = teamsRepository.save(newTeam);

        assertThat(savedTeam.getId()).isNotNull();
        assertThat(savedTeam.getName()).isEqualTo("New Team");
        assertThat(savedTeam.getAbbreviation()).isEqualTo("NEW");
    }
}