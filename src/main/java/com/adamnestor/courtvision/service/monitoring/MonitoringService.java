package com.adamnestor.courtvision.service.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class MonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    private final MeterRegistry meterRegistry;
    private final RestTemplate restTemplate;

    @Value("${monitoring.alert.endpoint:}")
    private String alertEndpoint;

    @Value("${monitoring.enabled:false}")
    private boolean monitoringEnabled;

    public MonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.restTemplate = new RestTemplate();
    }

    public void sendAlert(Map<String, Object> failureReport) {
        if (!monitoringEnabled) {
            logger.info("Monitoring disabled, skipping alert");
            return;
        }

        try {
            // Record metric
            meterRegistry.counter("cache.alerts").increment();

            // Send to monitoring system if configured
            if (alertEndpoint != null && !alertEndpoint.isEmpty()) {
                restTemplate.postForEntity(alertEndpoint, failureReport, String.class);
                logger.info("Alert sent to monitoring system: {}", alertEndpoint);
            }

            // Record specific metrics
            recordFailureMetrics(failureReport);

        } catch (Exception e) {
            logger.error("Failed to send monitoring alert: {}", e.getMessage());
            meterRegistry.counter("cache.alert.errors").increment();
        }
    }

    private void recordFailureMetrics(Map<String, Object> failureReport) {
        String updateType = (String) failureReport.get("updateType");
        meterRegistry.counter("cache.failures", "type", updateType).increment();

        if (failureReport.get("errorCount") instanceof Number errorCount) {
            meterRegistry.gauge("cache.error.count", errorCount.doubleValue());
        }

        if (failureReport.get("healthStatus") instanceof Boolean healthStatus) {
            meterRegistry.gauge("cache.health", healthStatus ? 1.0 : 0.0);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) failureReport.get("cacheMetrics");
        if (metrics != null) {
            if (metrics.get("hitRate") instanceof Number hitRate) {
                meterRegistry.gauge("cache.hit.rate", hitRate.doubleValue());
            }
            if (metrics.get("totalKeys") instanceof Number totalKeys) {
                meterRegistry.gauge("cache.keys.total", totalKeys.doubleValue());
            }
        }
    }
} 