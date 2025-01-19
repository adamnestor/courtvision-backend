package com.adamnestor.courtvision.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    @CacheEvict(value = {
        "playerStats", 
        "hitRates", 
        "confidenceScores", 
        "seasonStats"
    }, allEntries = true)
    @Scheduled(cron = "0 0 4 * * *", zone = "America/New_York")
    public void refreshCache() {
        // This method will be called at 4 AM ET every day
        // The @CacheEvict annotation will clear all caches
    }
} 