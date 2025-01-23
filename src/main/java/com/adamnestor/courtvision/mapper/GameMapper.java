package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.api.model.ApiGame;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface GameMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "externalId")
    @Mapping(target = "homeTeam", ignore = true)
    @Mapping(target = "awayTeam", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(source = "homeTeamScore", target = "homeTeamScore")
    @Mapping(source = "visitorTeamScore", target = "awayTeamScore")
    @Mapping(source = "date", target = "gameDate")
    @Mapping(source = "time", target = "gameTime")
    @Mapping(target = "status", expression = "java(apiGame.getStatus() != null && (apiGame.getStatus().contains(\"T\") || apiGame.getStatus().contains(\"ET\")) ? \"scheduled\" : apiGame.getStatus())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDate.now())")
    Games toEntity(ApiGame apiGame);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "id", target = "externalId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "homeTeam.id", target = "homeTeam.externalId")
    @Mapping(source = "visitorTeam.id", target = "awayTeam.externalId")
    @Mapping(target = "homeTeam.createdAt", ignore = true)
    @Mapping(target = "homeTeam.updatedAt", ignore = true)
    @Mapping(target = "awayTeam.createdAt", ignore = true)
    @Mapping(target = "awayTeam.updatedAt", ignore = true)
    @Mapping(source = "homeTeamScore", target = "homeTeamScore")
    @Mapping(source = "visitorTeamScore", target = "awayTeamScore")
    @Mapping(source = "date", target = "gameDate")
    @Mapping(source = "time", target = "gameTime")
    @Mapping(target = "status", expression = "java(apiGame.getStatus() != null && (apiGame.getStatus().contains(\"T\") || apiGame.getStatus().contains(\"ET\")) ? \"scheduled\" : apiGame.getStatus())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDate.now())")
    void updateEntity(@MappingTarget Games entity, ApiGame apiGame);
} 