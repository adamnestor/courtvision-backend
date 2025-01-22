package com.adamnestor.courtvision.config;

import com.adamnestor.courtvision.client.BallDontLieClient;
import com.adamnestor.courtvision.service.impl.CacheWarmingService;
import com.adamnestor.courtvision.service.impl.DataRefreshServiceImpl;
import com.adamnestor.courtvision.service.BallDontLieService;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@TestConfiguration
@EnableCaching
public class TestConfig {
    
    @MockBean
    private CacheWarmingService cacheWarmingService;
    
    @MockBean
    private DataRefreshServiceImpl dataRefreshService;
    
    @MockBean
    private BallDontLieService ballDontLieService;
    
    private MockWebServer mockWebServer;

    @PostConstruct
    void setUp() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
    }

    @PreDestroy
void tearDown() throws IOException {
    if (this.mockWebServer != null) {
        this.mockWebServer.shutdown();
    }
}

    @Bean
public BallDontLieClient ballDontLieClient() {
    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    return new BallDontLieClient(
        WebClient.builder(),
        "fake-api-key",
        baseUrl
    );
}

    @Bean
    public MockWebServer mockWebServer() {
        return this.mockWebServer;
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList("apiResponses", "games"));
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(100));
        return cacheManager;
    }
} 