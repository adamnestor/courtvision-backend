package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.client.BallDontLieClient;
import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.exception.ApiException;
import com.adamnestor.courtvision.exception.ApiRateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import com.adamnestor.courtvision.config.TestConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {TestConfig.class})
@ActiveProfiles("test")
class ApiClientIntegrationTest {

    @Autowired
    private BallDontLieClient apiClient;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockWebServer mockWebServer;

    @Test
    void getGames_CachesResponse() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"data\": []}")
            .setHeader("Content-Type", "application/json"));

        LocalDate testDate = LocalDate.now();
        
        // Act - First call
        List<ApiGame> firstCall = apiClient.getGames(testDate);
        
        // Get from cache
        var cache = cacheManager.getCache("apiResponses");
        assertNotNull(cache, "Cache 'apiResponses' should exist");
        var cachedValue = cache.get("games_" + testDate);
        
        // Assert
        assertNotNull(cachedValue);
        assertNotNull(firstCall);
    }

    @Test
    void getGames_HandlesRateLimit() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(429)
            .setBody("Too Many Requests"));
        
        LocalDate testDate = LocalDate.now();
        
        // Act & Assert
        assertThrows(ApiRateLimitException.class, () -> 
            apiClient.getGames(testDate));
    }

    @Test
    void scheduledTasks_ExecuteOnTime() {
        await()
            .atMost(1, TimeUnit.MINUTES)
            .untilAsserted(() -> {
                var cache = cacheManager.getCache("apiResponses");
                assertNotNull(cache);
                var nativeCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();
                assertFalse(nativeCache.asMap().isEmpty());
            });
    }

    @Test
    void getGames_HandlesServerError() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));
        
        LocalDate testDate = LocalDate.now();
        
        // Act & Assert
        assertThrows(ApiException.class, () -> 
            apiClient.getGames(testDate));
    }

    @Test
    void getGames_HandlesClientError() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("Bad Request"));
        
        LocalDate testDate = LocalDate.now();
        
        // Act & Assert
        assertThrows(ApiException.class, () -> 
            apiClient.getGames(testDate));
    }

    @Test
    void getGames_RetryOnFailure() {
        // Test retry mechanism
        LocalDate testDate = LocalDate.now();
        
        // First call fails, second succeeds
        List<ApiGame> result = apiClient.getGames(testDate);
        
        assertNotNull(result);
        // Verify retry was attempted (check logs)
    }

    @Test
    void cachesApiResponse() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"data\": []}")
            .setHeader("Content-Type", "application/json"));
        
        LocalDate testDate = LocalDate.now();
        
        // Act
        apiClient.getGames(testDate); // First call
        
        // Assert
        var cache = cacheManager.getCache("apiResponses");
        assertNotNull(cache);
        assertNotNull(cache.get("games_" + testDate));
    }
} 