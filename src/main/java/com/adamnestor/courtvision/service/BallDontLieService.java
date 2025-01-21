package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.api.model.ApiResponse;
import com.adamnestor.courtvision.api.model.ApiGameStats;
import com.adamnestor.courtvision.api.model.ApiPlayer;
import com.adamnestor.courtvision.api.model.ApiAdvancedStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

@Service
public class BallDontLieService {
    
    private final WebClient ballDontLieClient;

    @Autowired
    public BallDontLieService(WebClient ballDontLieClient) {
        this.ballDontLieClient = ballDontLieClient;
    }

    @Cacheable(value = "games", key = "#date")
    public List<ApiGame> getGames(LocalDate date) {
        return ballDontLieClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/games")
                .queryParam("dates[]", date.toString())
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiGame>>>() {})
            .block()
            .getData();
    }

    @Cacheable(value = "games", key = "'season-' + #season")
    public List<ApiGame> getGamesBySeason(Integer season) {
        return ballDontLieClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/games")
                .queryParam("seasons[]", season)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiGame>>>() {})
            .block()
            .getData();
    }

    @Cacheable(value = "gameStats", key = "#gameId")
    public List<ApiGameStats> getGameStats(Long gameId) {
        return ballDontLieClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/stats")
                .queryParam("game_ids[]", gameId)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiGameStats>>>() {})
            .block()
            .getData();
    }

    @Cacheable(value = "playerStats", key = "#playerId + '-' + #season")
    public List<ApiGameStats> getPlayerSeasonStats(Long playerId, Integer season) {
        return ballDontLieClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/stats")
                .queryParam("player_ids[]", playerId)
                .queryParam("seasons[]", season)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiGameStats>>>() {})
            .block()
            .getData();
    }

    @Cacheable(value = "players", key = "#playerId")
    public ApiPlayer getPlayer(Long playerId) {
        return ballDontLieClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/players/{id}")
                .build(playerId))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<ApiPlayer>>() {})
            .block()
            .getData();
    }

    @Cacheable(value = "players", key = "'active'")
    public List<ApiPlayer> getActivePlayers() {
        return ballDontLieClient.get()
            .uri("/players")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiPlayer>>>() {})
            .block()
            .getData();
    }

    @Cacheable(value = "players", key = "'team-' + #teamId")
    public List<ApiPlayer> getPlayersByTeam(Long teamId) {
        return ballDontLieClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/players")
                .queryParam("team_ids[]", teamId)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiPlayer>>>() {})
            .block()
            .getData();
    }

    @Cacheable(value = "players", key = "'search-' + #searchTerm")
    public List<ApiPlayer> searchPlayers(String searchTerm) {
        return ballDontLieClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/players")
                .queryParam("search", searchTerm)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiPlayer>>>() {})
            .block()
            .getData();
    }

    @Cacheable(value = "advancedStats", key = "#gameId")
    public List<ApiAdvancedStats> getAdvancedGameStats(Long gameId) {
        return ballDontLieClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/stats/advanced")
                .queryParam("game_ids[]", gameId)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiAdvancedStats>>>() {})
            .block()
            .getData();
    }

    @Cacheable(value = "advancedStats", key = "'player-' + #playerId + '-season-' + #season")
    public List<ApiAdvancedStats> getAdvancedSeasonStats(Long playerId, Integer season) {
        return ballDontLieClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/stats/advanced")
                .queryParam("player_ids[]", playerId)
                .queryParam("seasons[]", season)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiAdvancedStats>>>() {})
            .block()
            .getData();
    }
} 