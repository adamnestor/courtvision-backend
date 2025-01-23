package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.stats.StatsSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlayerStatsMapper {
    
    @Mapping(target = "team", expression = "java(player.getTeam().getAbbreviation())")
    @Mapping(target = "playerId", source = "player.id")
    @Mapping(target = "playerName", expression = "java(player.getFirstName() + \" \" + player.getLastName())")
    @Mapping(target = "gamesPlayed", source = "summary.successCount")
    PlayerDetailStats toPlayerDetailStats(
        Players player,
        StatsSummary summary,
        Integer threshold);
} 