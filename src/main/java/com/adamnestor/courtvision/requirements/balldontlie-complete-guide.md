# BallDontLie API Integration Guide

## API Information
- Base URL: https://api.balldontlie.io/v1/
- Authentication: Required via API key in headers
- Rate Limits: 
  - GOAT tier: 6000 requests/min
  - ALL-STAR tier: 600 requests/min
  - Free tier: 30 requests/min

## Required Endpoints & Implementation

### 1. Games Endpoint
```java
// Client Method
public ApiResponse<List<ApiGame>> getGames(LocalDate date) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/games")
            .queryParam("dates[]", date.toString())
            .build())
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiGame>>>() {})
        .block();
}

// DTO
public record ApiGame(
    Long id,
    LocalDate date,
    Integer season,
    String status,
    Integer period,
    String time,
    Integer homeTeamScore,
    Integer visitorTeamScore,
    ApiTeam homeTeam,
    ApiTeam visitorTeam
) {}
```
Used for:
- Getting scheduled games
- Retrieving final scores
- Tracking game dates

Key Parameters:
- `dates[]`: Get games for specific dates
- `team_ids[]`: Filter by teams
- `seasons[]`: Filter by seasons

### 2. Player Stats Endpoint
```java
// Client Methods
public ApiResponse<List<ApiGameStats>> getGameStats(Long gameId) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/stats")
            .queryParam("game_ids[]", gameId)
            .build())
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiGameStats>>>() {})
        .block();
}

public ApiResponse<List<ApiSeasonStats>> getSeasonAverages(Long playerId, Integer season) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/season_averages")
            .queryParam("season", season)
            .queryParam("player_id", playerId)
            .build())
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiSeasonStats>>>() {})
        .block();
}

// DTOs
public record ApiGameStats(
    Long id,
    String min,
    Integer points,
    Integer assists,
    Integer rebounds,
    Long playerId,
    Long gameId
) {}

public record ApiSeasonStats(
    Double pts,
    Double ast,
    Double reb,
    String min,
    Integer gamesPlayed,
    Long playerId,
    Integer season
) {}
```
Used for:
- Hit rate calculations
- Player performance tracking
- Historical analysis

### 3. Advanced Stats Endpoint
```java
// Client Method
public ApiResponse<List<ApiAdvancedStats>> getAdvancedStats(Long gameId) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/stats/advanced")
            .queryParam("game_ids[]", gameId)
            .build())
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiAdvancedStats>>>() {})
        .block();
}

// DTO
public record ApiAdvancedStats(
    BigDecimal pie,
    BigDecimal pace,
    BigDecimal assistPercentage,
    BigDecimal defensiveRating,
    BigDecimal netRating,
    BigDecimal offensiveRating,
    BigDecimal reboundPercentage,
    BigDecimal trueShootingPercentage,
    BigDecimal usagePercentage,
    Long playerId,
    Long gameId
) {}
```
Used for:
- Confidence score calculations
- Performance analysis
- Matchup evaluation

### 4. Active Players Endpoint
```java
// Client Method
public ApiResponse<List<ApiPlayer>> getActivePlayers() {
    return webClient.get()
        .uri("/players/active")
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ApiPlayer>>>() {})
        .block();
}

// DTO
public record ApiPlayer(
    Long id,
    String firstName,
    String lastName,
    String position,
    String teamId,
    String jerseyNumber
) {}
```
Used for:
- Current roster information
- Player identification
- Team associations

## Core Implementation Components

### 1. API Client Setup
```java
@Configuration
public class BallDontLieConfig {
    @Value("${balldontlie.api-key}")
    private String apiKey;

    @Bean
    public WebClient ballDontLieClient() {
        return WebClient.builder()
            .baseUrl("https://api.balldontlie.io/v1")
            .defaultHeader("Authorization", apiKey)
            .build();
    }
}
```

### 2. Common Response Wrapper
```java
public record ApiResponse<T>(
    T data,
    ApiMeta meta
) {}

public record ApiMeta(
    Integer nextCursor,
    Integer perPage
) {}
```

### 3. Error Handling
```java
@Slf4j
public class BallDontLieClient {
    private <T> T handleApiCall(Supplier<T> apiCall) {
        try {
            return apiCall.get();
        } catch (WebClientResponseException e) {
            log.error("API error: {} - {}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new ApiRateLimitException("Rate limit exceeded");
            }
            throw new ApiException("API call failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error calling API: {}", e.getMessage());
            throw new ApiException("Unexpected error: " + e.getMessage());
        }
    }
}
```

## Integration Notes

### Data Refresh Process
1. Fetch game results at 4am ET
```java
@Scheduled(cron = "0 0 4 * * *", zone = "America/New_York")
public void updateGameResults() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    List<ApiGame> games = apiClient.getGames(yesterday).data();
    games.forEach(this::processGameResults);
}
```

2. Update stats after games complete
```java
private void processGameResults(ApiGame game) {
    if ("Final".equals(game.status())) {
        updateGameStats(game.id());
        updateAdvancedStats(game.id());
    }
}
```

### Error Handling Strategy
- Retry on 5xx errors
- Back off on rate limits
- Log all API interactions
- Cache successful responses
- Fall back to last known data on failure

### Implementation Steps
1. Create API client with all required endpoints
2. Create complete set of DTOs needed
3. Add methods to existing services
4. Implement error handling and logging
5. Test all endpoints together

## Usage Example
```java
@Service
public class GameStatsService {
    private final BallDontLieClient apiClient;
    private final GameStatsRepository repository;

    public void updateStatsFromApi(Long gameId) {
        // Get basic stats
        List<ApiGameStats> gameStats = apiClient.getGameStats(gameId).data();
        
        // Get advanced stats
        List<ApiAdvancedStats> advancedStats = apiClient.getAdvancedStats(gameId).data();
        
        // Process and save
        for (var stat : gameStats) {
            GameStats stats = convertToGameStats(stat);
            AdvancedGameStats advanced = findMatchingAdvancedStats(stat.playerId(), advancedStats);
            stats.setAdvancedStats(advanced);
            repository.save(stats);
        }
    }
}
```