# CourtVision Backend

A sophisticated Spring Boot application providing NBA statistics analysis through integration with the BallDontLie API. Features advanced error handling, rate limiting, and comprehensive data mapping capabilities.

## 🌟 Key Features

### 🏀 NBA Data Integration
- Robust BallDontLie API integration
- Comprehensive error handling with automatic retries
- Support for pagination and cursor-based data retrieval

### 🔄 Smart Data Mapping
- MapStruct implementation for efficient object mapping
- Custom mapping strategies for complex data transformations
- Null-safe mapping with intelligent fallbacks

### 📊 Advanced Statistics Processing
- Comprehensive game statistics tracking
- Advanced metrics calculations
- Historical data analysis capabilities
- Support for various statistical thresholds:
  - Points: 10+, 15+, 20+, 25+
  - Assists: 2+, 4+, 6+, 8+
  - Rebounds: 4+, 6+, 8+, 10+

### 🛠 Technical Features
- Spring Boot 3.3.5 with Java 17
- WebFlux for reactive API calls
- Comprehensive API documentation with OpenAPI/Swagger

### 📈 Performance & Reliability
- Automatic retry mechanisms for API failures
- Rate limit handling with backoff strategies
- Comprehensive error logging and monitoring

## Technology Stack
- **Framework**: Spring Boot 3.3.5
- **Language**: Java 17
- **Data Access**: Spring Data JPA
- **API Documentation**: SpringDoc OpenAPI
- **Security**: Spring Security with JWT
- **Testing**: JUnit, Spring Boot Test
- **API Integration**: WebFlux WebClient
- **Object Mapping**: MapStruct

## Code Examples

### BallDontLie API Integration
```java
@Component
public class BallDontLieClient {
    private static final int MAX_RETRIES = 3;
    // Intelligent retry mechanism with backoff
    .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(10))
        .filter(throwable -> throwable instanceof ApiException
            && !(throwable instanceof ApiRateLimitException)
            && !(throwable instanceof ApiException)))
}
```

### Smart Data Mapping
```java
@Mapper(componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GameMapper {
    @Named("mapStatus")
    default String mapStatus(String status) {
        if (status == null) return null;
        return status.contains("T") || status.contains("ET") ? "scheduled" : status;
    }
}
```

### API Documentation Configuration
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("CourtVision API")
                .description("NBA Statistics Analysis API")
                .version("1.0"));
    }
}
```