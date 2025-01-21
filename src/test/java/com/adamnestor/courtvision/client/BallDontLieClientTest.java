package com.adamnestor.courtvision.client;

import com.adamnestor.courtvision.exception.ApiException;
import com.adamnestor.courtvision.exception.ApiRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BallDontLieClientTest {
    
    private BallDontLieClient client;
    private WebClient.Builder webClientBuilder;
    
    @BeforeEach
    void setUp() {
        webClientBuilder = mock(WebClient.Builder.class);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(mock(WebClient.class));
        
        client = new BallDontLieClient(webClientBuilder, "test-key", "http://test.com");
    }
    
    @Test
    void handleApiCall_RateLimitExceeded_ThrowsApiRateLimitException() {
        WebClientResponseException rateLimitException = 
            new WebClientResponseException(429, "Too Many Requests", null, null, null);
            
        assertThrows(ApiRateLimitException.class, () -> 
            client.handleApiCall(() -> { throw rateLimitException; }, "test-operation"));
    }
    
    @Test
    void handleApiCall_ServerError_ThrowsApiException() {
        WebClientResponseException serverError = 
            new WebClientResponseException(500, "Server Error", null, null, null);
            
        assertThrows(ApiException.class, () -> 
            client.handleApiCall(() -> { throw serverError; }, "test-operation"));
    }
} 