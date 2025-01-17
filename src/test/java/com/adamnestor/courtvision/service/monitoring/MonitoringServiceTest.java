package com.adamnestor.courtvision.service.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private MeterRegistry meterRegistry;
    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        monitoringService = new MonitoringService(meterRegistry);
        ReflectionTestUtils.setField(monitoringService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(monitoringService, "monitoringEnabled", true);
        ReflectionTestUtils.setField(monitoringService, "alertEndpoint", "http://test-endpoint");
    }

    @Test
    void sendAlert_WhenMonitoringEnabled_ShouldSendAlert() {
        // Given
        Map<String, Object> failureReport = createFailureReport();

        // When
        monitoringService.sendAlert(failureReport);

        // Then
        verify(restTemplate).postForEntity(anyString(), eq(failureReport), eq(String.class));
        assertEquals(1.0, meterRegistry.counter("cache.alerts").count());
    }

    @Test
    void sendAlert_WhenMonitoringDisabled_ShouldNotSendAlert() {
        // Given
        ReflectionTestUtils.setField(monitoringService, "monitoringEnabled", false);
        Map<String, Object> failureReport = createFailureReport();

        // When
        monitoringService.sendAlert(failureReport);

        // Then
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        assertEquals(0.0, meterRegistry.counter("cache.alerts").count());
    }

    @Test
    void sendAlert_WhenEndpointNotConfigured_ShouldOnlyRecordMetrics() {
        // Given
        ReflectionTestUtils.setField(monitoringService, "alertEndpoint", "");
        Map<String, Object> failureReport = createFailureReport();

        // When
        monitoringService.sendAlert(failureReport);

        // Then
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        assertEquals(1.0, meterRegistry.counter("cache.alerts").count());
    }

    @Test
    void sendAlert_WhenAlertFails_ShouldRecordError() {
        // Given
        Map<String, Object> failureReport = createFailureReport();
        doThrow(new RuntimeException("Alert failed"))
            .when(restTemplate)
            .postForEntity(anyString(), any(), any());

        // When
        monitoringService.sendAlert(failureReport);

        // Then
        assertEquals(1.0, meterRegistry.counter("cache.alert.errors").count());
    }

    @Test
    void sendAlert_ShouldRecordAllMetrics() {
        // Given
        Map<String, Object> failureReport = createFailureReport();
        failureReport.put("errorCount", 5);
        failureReport.put("healthStatus", true);
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("hitRate", 75.5);
        metrics.put("totalKeys", 100);
        failureReport.put("cacheMetrics", metrics);

        // When
        monitoringService.sendAlert(failureReport);

        // Then
        assertNotNull(meterRegistry.find("cache.error.count").gauge());
        assertNotNull(meterRegistry.find("cache.health").gauge());
        assertNotNull(meterRegistry.find("cache.hit.rate").gauge());
        assertNotNull(meterRegistry.find("cache.keys.total").gauge());
    }

    private Map<String, Object> createFailureReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("updateType", "test-update");
        report.put("timestamp", System.currentTimeMillis());
        return report;
    }
} 