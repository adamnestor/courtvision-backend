package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.api.model.ApiPlayer;
import org.mapstruct.*;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import com.adamnestor.courtvision.repository.TeamsRepository;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PlayerMapper {
    
    @Autowired @Lazy
    TeamsRepository teamsRepository = null;
    
    @Mapping(source = "id", target = "externalId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDate.now())")
    Players toEntity(ApiPlayer apiPlayer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "id", target = "externalId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDate.now())")
    void updateEntity(@MappingTarget Players entity, ApiPlayer apiPlayer);

    default LocalDate mapDate(LocalDate date) {
        return date != null ? date : LocalDate.now();
    }
}