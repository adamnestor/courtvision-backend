package com.adamnestor.courtvision.service.cache;

public interface CacheWarmingService {
    void warmTodaysPlayerCache();
    void warmTodaysGames();
    void warmHistoricalGames(java.time.LocalDate date);
}