# LocalDate Standardization Plan

## Overview

Standardize all date fields to use LocalDate for consistent Redis caching and reduced complexity. This includes audit fields since any entity might need caching. Store time components as strings where business requirements need them.

## Motivation

- Simplify Redis serialization
- Remove timezone complexity
- Provide consistent date handling across codebase
- Align with BallDontLie API date format (YYYY-MM-DD)

## Domain Changes Required

### Games

- `gameDate`: LocalDateTime → LocalDate
- `createdAt`: LocalDateTime → LocalDate
- `updatedAt`: LocalDateTime → LocalDate
- Add: `gameTime` (String)
- `status`: GameStatus enum → String (to match API's varied status values like "7:00 pm ET", "1st Qtr", "Final", etc)

### GameStats

- `createdAt`: LocalDateTime → LocalDate

### AdvancedGameStats

- `createdAt`: LocalDateTime → LocalDate

### HitRates

- `lastCalculated`: LocalDateTime → LocalDate
- `createdAt`: LocalDateTime → LocalDate

### UserPicks

- `createdAt`: LocalDateTime → LocalDate
- Add: `createdTime` (String)

### Users

- `lastLogin`: LocalDateTime → LocalDate
- Add: `lastLoginTime` (String)
- `createdAt`: LocalDateTime → LocalDate
- `updatedAt`: LocalDateTime → LocalDate

### Teams

- `createdAt`: LocalDateTime → LocalDate
- `updatedAt`: LocalDateTime → LocalDate

### Players

- `createdAt`: LocalDateTime → LocalDate
- `updatedAt`: LocalDateTime → LocalDate

## Implementation Steps

### 1. Update Domain Models

- Convert all date fields to LocalDate
- Add time fields where needed
- Update PrePersist/PreUpdate handlers
- Run compilation to identify dependencies

### 2. Cache Configuration

- Update CacheKeyGenerator
- Modify cache warming logic
- Simplify Redis serialization
- Update cache configuration

### 3. Service Layer

- Update all service methods using dates
- Modify date/time calculations
- Update API integration code
- Fix compilation errors

### 4. Repository Layer

- Update all JPA queries using dates
- Modify query method signatures
- Update test data setup

### 5. API Integration

- Update BallDontLie API service
- Handle date/time mapping
- Update response processing

## Implementation Checklist

### Domain Models

- [ ] Games
- [ ] GameStats
- [ ] AdvancedGameStats
- [ ] HitRates
- [ ] UserPicks
- [ ] Users
- [ ] Teams
- [ ] Players

### Infrastructure

- [ ] Redis configuration
- [ ] Cache key generation
- [ ] Entity listeners

### Services

- [ ] CacheWarmingService
- [ ] StatsCacheService
- [ ] BallDontLieApiService

### Repositories

- [ ] GameStatsRepository
- [ ] GamesRepository
- [ ] HitRatesRepository
- [ ] PlayersRepository
- [ ] TeamsRepository
- [ ] UsersRepository

### Tests

- [ ] Entity tests
- [ ] Repository tests
- [ ] Service tests
- [ ] Cache tests
- [ ] API integration tests

## Testing Strategy

### Unit Tests

- Update test data builders
- Modify date-based assertions
- Add cache serialization tests
- Test time string formatting

### Integration Tests

- Test Redis caching with dates
- Verify API date handling
- Test repository queries
- Validate date/time display

### API Tests

- Test BallDontLie integration
- Verify date format consistency
- Test time component handling

## Future Considerations

### Performance

- Monitor cache hit rates
- Track serialization performance
- Watch database query execution plans

### Maintenance

- Document date/time handling standards
- Add date format validation
- Consider date formatting utilities
- Monitor for any timezone issues
