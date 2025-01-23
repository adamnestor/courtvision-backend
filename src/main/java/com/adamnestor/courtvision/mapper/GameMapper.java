package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.api.model.ApiGame;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.Named;
import com.adamnestor.courtvision.repository.TeamsRepository;
import com.adamnestor.courtvision.domain.Teams;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {TeamsRepository.class})
public interface GameMapper {
    
    @Autowired
    TeamsRepository teamsRepository = null;
    
    @Mapping(source = "homeTeam.id", target = "homeTeam.externalId")
    @Mapping(source = "visitorTeam.id", target = "awayTeam.externalId")
    @Mapping(target = "homeTeam.createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "homeTeam.updatedAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "awayTeam.createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "awayTeam.updatedAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(source = "id", target = "externalId")
    @Mapping(source = "date", target = "gameDate")
    @Mapping(source = "time", target = "gameTime")
    @Mapping(source = "homeTeamScore", target = "homeTeamScore")
    @Mapping(source = "visitorTeamScore", target = "awayTeamScore")
    @Mapping(target = "status", expression = "java(apiGame.getStatus() != null && (apiGame.getStatus().contains(\"T\") || apiGame.getStatus().contains(\"ET\")) ? \"scheduled\" : apiGame.getStatus())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDate.now())")
    Games toEntity(ApiGame apiGame);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "homeTeam.id", target = "homeTeam.externalId")
    @Mapping(source = "visitorTeam.id", target = "awayTeam.externalId")
    @Mapping(target = "homeTeam.createdAt", ignore = true)
    @Mapping(target = "homeTeam.updatedAt", ignore = true)
    @Mapping(target = "awayTeam.createdAt", ignore = true)
    @Mapping(target = "awayTeam.updatedAt", ignore = true)
    @Mapping(source = "id", target = "externalId")
    @Mapping(source = "date", target = "gameDate")
    @Mapping(source = "time", target = "gameTime")
    @Mapping(source = "homeTeamScore", target = "homeTeamScore")
    @Mapping(source = "visitorTeamScore", target = "awayTeamScore")
    @Mapping(target = "status", expression = "java(apiGame.getStatus() != null && (apiGame.getStatus().contains(\"T\") || apiGame.getStatus().contains(\"ET\")) ? \"scheduled\" : apiGame.getStatus())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDate.now())")
    void updateEntity(@MappingTarget Games entity, ApiGame apiGame);

    @Named("teamIdToTeam")
    default Teams teamIdToTeam(Long teamId) {
        if (teamId == null) {
            return null;
        }
        return teamsRepository.findByExternalId(teamId).orElse(null);
    }
} 