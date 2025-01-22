package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.api.model.*;
import java.time.LocalDate;
import java.util.List;

public interface BallDontLieService {
    List<ApiGame> getGames(LocalDate date);
    List<ApiGame> getGamesBySeason(Integer season);
    List<ApiTeam> getAllTeams();
    List<ApiPlayer> getAllPlayers();
    ApiPlayer getPlayer(Long playerId);
    List<ApiPlayer> getPlayersByTeam(Long teamId);
    List<ApiGameStats> getGameStats(Long gameId);
    List<ApiAdvancedStats> getAdvancedGameStats(Long gameId);
    List<ApiAdvancedStats> getAdvancedSeasonStats(Long playerId, Integer season);
    List<ApiGameStats> getPlayerSeasonStats(Long playerId, Integer season);
} 