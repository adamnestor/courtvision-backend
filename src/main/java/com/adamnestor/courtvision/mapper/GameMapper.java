package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.domain.Games;
import org.springframework.stereotype.Component;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.Mapper;

@Component
@Mapper(componentModel = "spring")
public interface GameMapper {
    
    @Mapping(source = "id", target = "externalId")
    @Mapping(source = "homeTeam.id", target = "homeTeam.externalId")
    @Mapping(source = "visitorTeam.id", target = "awayTeam.externalId")
    @Mapping(target = "homeTeam.createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "homeTeam.updatedAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "awayTeam.createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "awayTeam.updatedAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(source = "homeTeamScore", target = "homeTeamScore")
    @Mapping(source = "visitorTeamScore", target = "awayTeamScore")
    @Mapping(source = "date", target = "gameDate")
    @Mapping(source = "time", target = "gameTime")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDate.now())")
    Games toEntity(ApiGame apiGame);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "id", target = "externalId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "homeTeam.id", target = "homeTeam.externalId")
    @Mapping(source = "visitorTeam.id", target = "awayTeam.externalId")
    @Mapping(target = "homeTeam.createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "homeTeam.updatedAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "awayTeam.createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "awayTeam.updatedAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(source = "homeTeamScore", target = "homeTeamScore")
    @Mapping(source = "visitorTeamScore", target = "awayTeamScore")
    @Mapping(source = "date", target = "gameDate")
    @Mapping(source = "time", target = "gameTime")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDate.now())")
    void updateEntity(@MappingTarget Games entity, ApiGame apiGame);
} 