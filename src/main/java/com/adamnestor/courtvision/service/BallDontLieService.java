package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.api.model.ApiTeam;
import com.adamnestor.courtvision.api.model.ApiPlayer;
import com.adamnestor.courtvision.api.model.ApiGameStats;
import com.adamnestor.courtvision.api.model.ApiAdvancedStats;
import com.adamnestor.courtvision.client.BallDontLieClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class BallDontLieService {

    private static final Logger logger = LoggerFactory.getLogger(BallDontLieService.class);

    @Autowired
    private BallDontLieClient apiClient;

    public List<ApiGame> getGames(LocalDate date) {
        return apiClient.getGames(date);
    }

    public List<ApiGame> getGamesByYearMonth(int year, int month) {
        return apiClient.getGamesByYearMonth(year, month);
    }

    public List<ApiTeam> getAllTeams() {
        return apiClient.getAllTeams();
    }

    public List<ApiPlayer> getAllPlayers() {
        logger.info("Fetching active roster players from /players/active endpoint");
        return apiClient.getAllPlayers();
    }

    public ApiPlayer getPlayer(Long playerId) {
        try {
            ApiPlayer player = apiClient.getPlayer(playerId);
            if (player == null) {
                logger.warn("No player data returned for ID: {}", playerId);
                return null;
            }
            return player;
        } catch (Exception e) {
            logger.error("Error fetching player {}: {}", playerId, e.getMessage());
            return null;
        }
    }

    public List<ApiPlayer> getPlayersByTeam(Long teamId) {
        return apiClient.getPlayersByTeam(teamId);
    }

    public List<ApiGameStats> getGameStats(Long gameId) {
        return apiClient.getGameStats(gameId);
    }

    public List<ApiAdvancedStats> getAdvancedGameStats(Long gameId) {
        return apiClient.getAdvancedGameStats(gameId);
    }

    public List<ApiAdvancedStats> getAdvancedSeasonStats(Long playerId, Integer season) {
        return apiClient.getAdvancedSeasonStats(playerId, season);
    }

    public List<ApiGameStats> getPlayerSeasonStats(Long playerId, Integer season) {
        return apiClient.getPlayerSeasonStats(playerId, season);
    }

    public List<ApiGame> getGamesByDateRange(LocalDate startDate, LocalDate endDate) {
        return apiClient.getGamesByDateRange(startDate, endDate);
    }
}