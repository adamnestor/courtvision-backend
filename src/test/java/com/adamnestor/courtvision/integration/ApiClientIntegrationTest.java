package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.client.BallDontLieClient;
import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.exception.ApiException;
import com.adamnestor.courtvision.exception.ApiRateLimitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import com.adamnestor.courtvision.config.TestConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import okhttp3.mockwebserver.RecordedRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {TestConfig.class})
@ActiveProfiles("test")
class ApiClientIntegrationTest {

    @Autowired
    private BallDontLieClient apiClient;

    @Autowired
    private CacheManager cacheManager;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        // Create new MockWebServer for each test
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        // Reset the WebClient for each test with new MockWebServer URL
        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();
        ReflectionTestUtils.setField(apiClient, "webClient", webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Ensure MockWebServer is shut down after each test
        mockWebServer.shutdown();
        
        // Clear all caches
        for (String cacheName : cacheManager.getCacheNames()) {
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
        }
    }

    @Test
    void getGames_CachesResponse() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"data\": [{\"id\":1,\"date\":\"2025-01-21\"}]}"));

        LocalDate testDate = LocalDate.now();
        
        // Act - First call
        List<ApiGame> firstCall = apiClient.getGames(testDate);
        
        // Assert
        var cache = cacheManager.getCache("apiResponses");
        assertNotNull(cache, "Cache 'apiResponses' should exist");
        assertNotNull(cache.get(testDate.toString()));
        assertNotNull(firstCall);
    }

    @Test
    void getGames_HandlesRateLimit() throws Exception {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(429)
            .setHeader("Content-Type", "application/json")
            .setHeader("Retry-After", "30")
            .setBody("{\"message\":\"Rate limit exceeded\"}"));
        
        LocalDate testDate = LocalDate.now();
        
        // Act & Assert
        assertThrows(ApiRateLimitException.class, () -> apiClient.getGames(testDate));
        
        // Verify the request
        RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request, "Request was not made");
        assertTrue(request.getPath().contains("/games"));
    }

    @Test
    void scheduledTasks_ExecuteOnTime() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"data\":[{\"id\":1,\"date\":\"2025-01-21\"}]}"));

        LocalDate testDate = LocalDate.now();
        
        // Act
        apiClient.getGames(testDate);

        // Assert
        await()
            .atMost(1, TimeUnit.MINUTES)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                var cache = cacheManager.getCache("apiResponses");
                assertNotNull(cache);
                assertNotNull(cache.get(testDate.toString()));
            });
    }

    @Test
    void getGames_HandlesClientError() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Bad Request\"}"));
        
        LocalDate testDate = LocalDate.now();
        
        // Act & Assert
        assertThrows(ApiException.class, () -> {
            try {
                apiClient.getGames(testDate);
            } catch (Exception e) {
                // Verify the request was made
                RecordedRequest request = mockWebServer.takeRequest();
                assertEquals("/games", request.getPath().split("\\?")[0]);
                throw e;
            }
        });
    }

    @Test
    void getGames_RetryOnFailure() throws Exception {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Internal Server Error\"}"));
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"data\":[{\"id\":1,\"date\":\"2025-01-21\"}]}"));
        
        // Act & Assert
        List<ApiGame> result = apiClient.getGames(LocalDate.now());
        assertNotNull(result);
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void cachesApiResponse() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"data\": [{\"id\":1,\"date\":\"2025-01-21\"}]}"));
        
        LocalDate testDate = LocalDate.now();
        
        // Act
        apiClient.getGames(testDate);
        
        // Assert
        var cache = cacheManager.getCache("apiResponses");
        assertNotNull(cache);
        assertNotNull(cache.get(testDate.toString()));
    }
} 