package com.adamnestor.courtvision.client;

import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.api.model.ApiResponse;
import com.adamnestor.courtvision.exception.ApiException;
import com.adamnestor.courtvision.exception.ApiRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;
import java.time.LocalDate;
import java.util.List;

@Component
public class BallDontLieClient {
    private static final Logger log = LoggerFactory.getLogger(BallDontLieClient.class);
    private final WebClient webClient;

    public BallDontLieClient(WebClient.Builder webClientBuilder, 
                            @Value("${balldontlie.api-key}") String apiKey,
                            @Value("${balldontlie.base-url}") String baseUrl) {
        this.webClient = webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", apiKey)
            .build();
    }

    @Cacheable(value = "apiResponses", unless = "#result == null")
    protected <T> T handleApiCall(Supplier<T> apiCall, String operation) {
        try {
            log.debug("Executing API operation: {}", operation);
            return apiCall.get();
        } catch (WebClientResponseException e) {
            log.error("API error during {}: {} - {}", operation, e.getStatusCode(), e.getMessage());
            
            if (e.getStatusCode().value() == 429) {
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

    @Cacheable(value = "apiResponses", key = "#date.toString()")
    public List<ApiGame> getGames(LocalDate date) {
        log.debug("Executing API operation: getGames for date: {}", date);
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/games")
                .queryParam("dates[]", date.toString())
                .build())
            .retrieve()
            .onStatus(status -> status.is4xxClientError(), response -> {
                if (response.statusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
                    log.warn("Rate limit exceeded");
                    return Mono.error(new ApiRateLimitException("Rate limit exceeded"));
                }
                log.error("Client error: {}", response.statusCode());
                return Mono.error(new ApiException("Client error: " + response.statusCode()));
            })
            .onStatus(status -> status.is5xxServerError(), response -> {
                log.error("Server error: {}", response.statusCode());
                return Mono.error(new ApiException("Server error: " + response.statusCode()));
            })
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiGame>>>() {})
            .block()
            .getData();
    }
} 