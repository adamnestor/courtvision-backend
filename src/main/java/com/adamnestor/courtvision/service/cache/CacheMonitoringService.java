package com.adamnestor.courtvision.service.cache;

import org.springframework.stereotype.Service;

@Service
public interface CacheMonitoringService {
    void recordError(Exception e);
    boolean performHealthCheck();
    double getHitRate();
    double getErrorRate();
    void recordCacheAccess(boolean isHit);
    boolean checkHealth();
    double getMissRate();
}