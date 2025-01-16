package com.adamnestor.courtvision.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adamnestor.courtvision.service.cache.CacheMonitoringService;

@Service
public class CacheIntegrationService {
    
    private static final Logger log = LoggerFactory.getLogger(CacheIntegrationService.class);
    
    @Autowired
    private DailyRefreshService dailyRefreshService;
    
    @Autowired
    private WarmingStrategyService warmingStrategyService;
    
    @Autowired
    private CacheMonitoringService monitoringService;
    
    public void performDailyUpdate() {
        log.info("Starting daily cache update process");
        
        // Perform health check
        if (!performHealthCheck()) {
            log.error("Health check failed, aborting daily update");
            return;
        }
        
        try {
            // Execute daily refresh
            dailyRefreshService.performDailyRefresh();
            
            // Warm cache with high priority data
            warmingStrategyService.executeWarmingStrategy(WarmingStrategyService.WarmingPriority.HIGH);
            
            // Verify data synchronization
            verifyDataSynchronization();
            
            log.info("Daily cache update completed successfully");
        } catch (Exception e) {
            log.error("Error during daily cache update: {}", e.getMessage());
            handleUpdateFailure(e);
        }
    }
    
    private boolean performHealthCheck() {
        return monitoringService.checkHealth();
    }
    
    private void verifyDataSynchronization() {
        // Implementation for data synchronization verification
    }
    
    private void handleUpdateFailure(Exception e) {
        // Implementation for handling update failures
    }
} 