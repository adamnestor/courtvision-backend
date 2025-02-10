package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.repository.TeamsRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import java.time.LocalDate;

@Mapper(componentModel = "spring", 
        uses = {TeamsRepository.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        builder = @Builder(disableBuilder = true))
public interface GameMapper {
    
    @Autowired @Lazy
    TeamsRepository teamsRepository = null;
    
    @Mapping(source = "homeTeam.id", target = "homeTeam.externalId")
    @Mapping(source = "visitorTeam.id", target = "awayTeam.externalId")
    @Mapping(target = "homeTeam.createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "homeTeam.updatedAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "awayTeam.createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "awayTeam.updatedAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(source = "id", target = "externalId")
    @Mapping(source = "date", target = "gameDate", qualifiedByName = "mapGameDate")
    @Mapping(source = "time", target = "gameTime", qualifiedByName = "mapGameTime")
    @Mapping(source = "homeTeamScore", target = "homeTeamScore")
    @Mapping(source = "visitorTeamScore", target = "awayTeamScore")
    @Mapping(target = "status", qualifiedByName = "mapStatus")
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
    @Mapping(source = "date", target = "gameDate", qualifiedByName = "mapGameDate")
    @Mapping(source = "time", target = "gameTime", qualifiedByName = "mapGameTime")
    @Mapping(source = "homeTeamScore", target = "homeTeamScore")
    @Mapping(source = "visitorTeamScore", target = "awayTeamScore")
    @Mapping(target = "status", expression = "java(mapStatus(apiGame.getStatus()))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDate.now())")
    void updateEntity(@MappingTarget Games entity, ApiGame apiGame);

    @Named("mapGameTime")
    default String mapGameTime(String time) {
        if (time == null || time.equals("Final") || time.isEmpty()) {
            return null;
        }
        return time;
    }

    @Named("mapGameDate")
    default LocalDate mapGameDate(LocalDate date) {
        return date != null ? date : LocalDate.now();
    }

    @Named("mapStatus")
    default String mapStatus(String status) {
        if (status == null) return null;
        return status.contains("T") || status.contains("ET") ? "scheduled" : status;
    }

    @Named("teamIdToTeam")
    default Teams teamIdToTeam(Long teamId) {
        if (teamId == null) {
            return null;
        }
        return teamsRepository.findByExternalId(teamId).orElse(null);
    }
} 