package com.adamnestor.courtvision.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "cache.alerts")
public class CacheAlertConfig {
    private Map<String, CacheThresholds> thresholds = new HashMap<>();

    public static class CacheThresholds {
        private double hitRate = 0.6;
        private double size = 0.9;
        private long evictionRate = 100;
        private long operationThresholdMs = 1000; // 1 second threshold

        // Getters and setters
        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }
        public double getSize() { return size; }
        public void setSize(double size) { this.size = size; }
        public long getEvictionRate() { return evictionRate; }
        public void setEvictionRate(long evictionRate) { this.evictionRate = evictionRate; }
        public long getOperationThresholdMs() { return operationThresholdMs; }
        public void setOperationThresholdMs(long operationThresholdMs) { 
            this.operationThresholdMs = operationThresholdMs; 
        }
    }

    public Map<String, CacheThresholds> getThresholds() { return thresholds; }
    public void setThresholds(Map<String, CacheThresholds> thresholds) { this.thresholds = thresholds; }
} 