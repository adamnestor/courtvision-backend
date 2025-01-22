package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.api.model.ApiTeam;
import com.adamnestor.courtvision.domain.Conference;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.repository.TeamsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {
    private final TeamsRepository teamsRepository;
    private static final Logger logger = LoggerFactory.getLogger(TeamMapper.class);

    public TeamMapper(TeamsRepository teamsRepository) {
        this.teamsRepository = teamsRepository;
    }

    public Teams toEntity(ApiTeam apiTeam) {
        if (apiTeam == null) {
            return null;
        }

        // First try to find existing team
        Teams team = teamsRepository.findByExternalId(apiTeam.getId())
            .orElse(new Teams());

        // Update fields
        team.setExternalId(apiTeam.getId());
        team.setName(apiTeam.getName());
        team.setAbbreviation(apiTeam.getAbbreviation());
        team.setCity(apiTeam.getCity());
        team.setConference(mapConference(apiTeam.getConference()));
        team.setDivision(apiTeam.getDivision());

        // Save and return
        logger.debug("Saving team: {}", team);
        return teamsRepository.save(team);
    }

    public void updateEntity(Teams existingTeam, ApiTeam apiTeam) {
        if (apiTeam == null) {
            return;
        }

        existingTeam.setName(apiTeam.getName());
        existingTeam.setAbbreviation(apiTeam.getAbbreviation());
        existingTeam.setCity(apiTeam.getCity());
        existingTeam.setConference(mapConference(apiTeam.getConference()));
        existingTeam.setDivision(apiTeam.getDivision());
    }

    private Conference mapConference(String conferenceStr) {
        if (conferenceStr == null) {
            return null;
        }
        return switch (conferenceStr.toLowerCase()) {
            case "east", "eastern" -> Conference.East;
            case "west", "western" -> Conference.West;
            default -> null;
        };
    }
} 