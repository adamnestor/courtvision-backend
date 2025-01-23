package com.adamnestor.courtvision.client;

import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.api.model.ApiTeam;
import com.adamnestor.courtvision.api.model.ApiPlayer;
import com.adamnestor.courtvision.api.model.ApiGameStats;
import com.adamnestor.courtvision.api.model.ApiResponse;
import com.adamnestor.courtvision.api.model.ApiAdvancedStats;
import com.adamnestor.courtvision.exception.ApiException;
import com.adamnestor.courtvision.exception.ApiRateLimitException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.function.Supplier;
import java.time.LocalDate;
import java.util.List;
import java.time.Duration;
import java.util.ArrayList;

@Component
public class BallDontLieClient {
    private static final Logger log = LoggerFactory.getLogger(BallDontLieClient.class);
    private static final int MAX_RETRIES = 3;
    private static final String baseUrl = "https://www.balldontlie.io/api/v1";
    private final WebClient webClient;

    public BallDontLieClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Cacheable(value = "apiResponses", key = "#date.toString()")
    public List<ApiGame> getGames(LocalDate date) {
        log.debug("Executing API operation: getGames for date: {}", date);
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/games")
                .queryParam("dates[]", date.toString())
                .build())
            .retrieve()
            .onStatus(status -> status.is5xxServerError(), response -> {
                log.error("Server error: {}", response.statusCode());
                return Mono.error(new ApiException("Server error: " + response.statusCode()));
            })
            .onStatus(status -> status.value() == 429, response -> {
                log.warn("Rate limit exceeded");
                return Mono.error(new ApiRateLimitException("Rate limit exceeded"));
            })
            .onStatus(status -> status.is4xxClientError(), response -> {
                log.error("Client error: {}", response.statusCode());
                return Mono.error(new ApiException("Client error: " + response.statusCode()));
            })
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiGame>>>() {})
            .map(ApiResponse::getData)
            .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(10))
                .filter(throwable -> throwable instanceof ApiException 
                    && !(throwable instanceof ApiRateLimitException)
                    && !(throwable instanceof ApiException && throwable.getMessage().contains("Client error"))))
            .block();
    }

    @Cacheable(value = "apiResponses", unless = "#result == null")
    protected <T> T handleApiCall(Supplier<T> apiCall, String operation) {
        try {
            log.debug("Executing API operation: {}", operation);
            return apiCall.get();
        } catch (WebClientResponseException e) {
            log.error("API error during {}: {} - {}", operation, e.getStatusCode(), e.getMessage());
            
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Rate limit exceeded during {}", operation);
                throw new ApiRateLimitException("Rate limit exceeded", e);
            }
            
            if (e.getStatusCode().is5xxServerError()) {
                log.error("Server error during {}, will retry", operation);
                throw new ApiException("Server error: " + e.getMessage(), e);
            }
            
            throw new ApiException("API call failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during {}: {}", operation, e.getMessage());
            throw new ApiException("Unexpected error: " + e.getMessage(), e);
        }
    }

    public WebClient.RequestHeadersUriSpec<?> get() {
        return webClient.get();
    }

    @Cacheable(value = "teams", unless = "#result == null")
    public List<ApiTeam> getAllTeams() {
        return handleApiCall(() -> webClient.get()
            .uri("/teams")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiTeam>>>() {})
            .block()
            .getData(),
            "getAllTeams");
    }

    @Cacheable(value = "players", unless = "#result == null")
    public List<ApiPlayer> getAllPlayers() {
        return handleApiCall(() -> webClient.get()
            .uri("/players")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiPlayer>>>() {})
            .block()
            .getData(),
            "getAllPlayers");
    }

    @Cacheable(value = "players", unless = "#result == null")
    public ApiPlayer getPlayer(Long id) {
        return handleApiCall(() -> {
            log.debug("Fetching player with ID: {}", id);
            var response = webClient.get()
                .uri("/players/" + id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<ApiPlayer>>() {})
                .block();
            if (response != null) {
                Object rawData = response.getData();
                log.debug("Raw response data type for player {}: {}", id, 
                    rawData != null ? rawData.getClass().getName() : "null");
                if (rawData instanceof List) {
                    log.warn("Received List instead of single player for ID: {}", id);
                    List<?> dataList = (List<?>) rawData;
                    if (!dataList.isEmpty()) {
                        return (ApiPlayer) dataList.get(0);  // Take first player if multiple returned
                    }
                }
            }
            return response != null ? response.getData() : null;
        }, "getPlayer");
    }

    @Cacheable(value = "players", unless = "#result == null")
    public List<ApiPlayer> getPlayersByTeam(Long teamId) {
        return handleApiCall(() -> webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/players")
                .queryParam("team_ids[]", teamId)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiPlayer>>>() {})
            .block()
            .getData(),
            "getPlayersByTeam");
    }

    @Cacheable(value = "gameStats", unless = "#result == null")
    public List<ApiGameStats> getGameStats(Long gameId) {
        return handleApiCall(() -> webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/stats")
                .queryParam("game_ids[]", gameId)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiGameStats>>>() {})
            .block()
            .getData(),
            "getGameStats");
    }

    @Cacheable(value = "advancedStats", unless = "#result == null")
    public List<ApiAdvancedStats> getAdvancedGameStats(Long gameId) {
        return handleApiCall(() -> webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/stats/advanced")
                .queryParam("game_ids[]", gameId)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiAdvancedStats>>>() {})
            .block()
            .getData(),
            "getAdvancedGameStats");
    }

    @Cacheable(value = "advancedStats", unless = "#result == null")
    public List<ApiAdvancedStats> getAdvancedSeasonStats(Long playerId, Integer season) {
        return handleApiCall(() -> webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/stats/advanced")
                .queryParam("player_ids[]", playerId)
                .queryParam("seasons[]", season)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiAdvancedStats>>>() {})
            .block()
            .getData(),
            "getAdvancedSeasonStats");
    }

    @Cacheable(value = "games", unless = "#result == null")
    public List<ApiGame> getGamesByYearMonth(int year, int month) {
        String url = String.format("%s/games?seasons[]=%d&start_date=%d-%02d-01&end_date=%d-%02d-31",
            baseUrl, year, year, month, year, month);
        
        return fetchAllPages(url, ApiGame.class);
    }

    @Cacheable(value = "playerStats", unless = "#result == null")
    public List<ApiGameStats> getPlayerSeasonStats(Long playerId, Integer season) {
        return handleApiCall(() -> webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/stats")
                .queryParam("player_ids[]", playerId)
                .queryParam("seasons[]", season)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiGameStats>>>() {})
            .block()
            .getData(),
            "getPlayerSeasonStats");
    }

    public List<ApiGame> getGamesByDateRange(LocalDate startDate, LocalDate endDate) {
        List<ApiGame> allGames = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            allGames.addAll(getGames(currentDate));
            currentDate = currentDate.plusDays(1);
        }
        
        return allGames;
    }

    private <T> List<T> fetchAllPages(String url, Class<T> type) {
        List<T> allItems = new ArrayList<>();
        Integer nextCursor = null;
        
        do {
            String pageUrl = nextCursor == null ? url : url + "&cursor=" + nextCursor;
            var response = handleApiCall(() -> webClient.get()
                .uri(pageUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<T>>>() {})
                .block(),
                "fetchAllPages");
            
            if (response != null && response.getData() != null) {
                allItems.addAll(response.getData());
                nextCursor = response.getMeta() != null ? response.getMeta().getNext_cursor() : null;
            }
        } while (nextCursor != null);
        
        return allItems;
    }
} 