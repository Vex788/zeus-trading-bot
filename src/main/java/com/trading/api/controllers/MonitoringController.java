package com.trading.api.controllers;

import com.trading.config.AuditService;
import com.trading.config.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for monitoring and metrics
 */
@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {
    
    @Autowired
    private MetricsService metricsService;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Get system health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        try {
            MetricsService.SystemHealth health = metricsService.getSystemHealth();
            
            return ResponseEntity.ok(Map.of(
                "status", health.getStatus().toString(),
                "totalRequests", health.getTotalRequests(),
                "totalErrors", health.getTotalErrors(),
                "errorRate", String.format("%.2f%%", health.getErrorRate() * 100),
                "averageResponseTime", String.format("%.2fms", health.getAverageResponseTime()),
                "timestamp", health.getTimestamp().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get system health: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get performance metrics
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        try {
            MetricsService.PerformanceSummary performance = metricsService.getPerformanceSummary();
            
            return ResponseEntity.ok(Map.of(
                "totalMethods", performance.getTotalMethods(),
                "averageResponseTime", String.format("%.2fms", performance.getAverageResponseTime()),
                "totalSuccessfulOperations", performance.getTotalSuccessfulOperations(),
                "totalFailedOperations", performance.getTotalFailedOperations(),
                "topSlowMethods", performance.getTopSlowMethods()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get performance metrics: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get trading metrics
     */
    @GetMapping("/trading")
    public ResponseEntity<Map<String, Object>> getTradingMetrics() {
        try {
            MetricsService.TradingSummary trading = metricsService.getTradingSummary();
            
            return ResponseEntity.ok(Map.of(
                "totalTrades", trading.getTotalTrades(),
                "successfulTrades", trading.getSuccessfulTrades(),
                "successRate", String.format("%.2f%%", trading.getSuccessRate() * 100),
                "averageTradingTime", String.format("%.2fms", trading.getAverageTradingTime())
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get trading metrics: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get recent audit events
     */
    @GetMapping("/audit/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentAuditEvents(
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<AuditService.AuditEvent> events = auditService.getRecentEvents(limit);
            
            List<Map<String, Object>> eventData = events.stream()
                .map(event -> Map.of(
                    "id", event.getId(),
                    "type", event.getEventType().toString(),
                    "userId", event.getUserId(),
                    "details", event.getDetails(),
                    "ipAddress", event.getIpAddress(),
                    "timestamp", event.getTimestamp().toString()
                ))
                .toList();
            
            return ResponseEntity.ok(eventData);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get security events by type
     */
    @GetMapping("/audit/security")
    public ResponseEntity<List<Map<String, Object>>> getSecurityEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AuditService.AuditEvent> events;
            
            if (eventType != null) {
                AuditService.SecurityEventType type = AuditService.SecurityEventType.valueOf(eventType);
                events = auditService.getEventsByType(type, limit);
            } else {
                events = auditService.getRecentEvents(limit);
            }
            
            List<Map<String, Object>> eventData = events.stream()
                .map(event -> Map.of(
                    "id", event.getId(),
                    "type", event.getEventType().toString(),
                    "userId", event.getUserId(),
                    "details", event.getDetails(),
                    "ipAddress", event.getIpAddress(),
                    "timestamp", event.getTimestamp().toString()
                ))
                .toList();
            
            return ResponseEntity.ok(eventData);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get system metrics summary
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        try {
            MetricsService.SystemHealth health = metricsService.getSystemHealth();
            MetricsService.PerformanceSummary performance = metricsService.getPerformanceSummary();
            MetricsService.TradingSummary trading = metricsService.getTradingSummary();
            
            return ResponseEntity.ok(Map.of(
                "health", Map.of(
                    "status", health.getStatus().toString(),
                    "errorRate", health.getErrorRate(),
                    "averageResponseTime", health.getAverageResponseTime()
                ),
                "performance", Map.of(
                    "totalMethods", performance.getTotalMethods(),
                    "successfulOperations", performance.getTotalSuccessfulOperations(),
                    "failedOperations", performance.getTotalFailedOperations()
                ),
                "trading", Map.of(
                    "totalTrades", trading.getTotalTrades(),
                    "successRate", trading.getSuccessRate(),
                    "averageTradingTime", trading.getAverageTradingTime()
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get metrics summary: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get available event types for filtering
     */
    @GetMapping("/audit/event-types")
    public ResponseEntity<List<String>> getEventTypes() {
        try {
            List<String> eventTypes = java.util.Arrays.stream(AuditService.SecurityEventType.values())
                .map(Enum::toString)
                .toList();
            
            return ResponseEntity.ok(eventTypes);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Record a custom metric (for testing or manual tracking)
     */
    @PostMapping("/metrics/record")
    public ResponseEntity<Map<String, Object>> recordCustomMetric(
            @RequestBody Map<String, Object> metricData) {
        try {
            String metricName = (String) metricData.get("name");
            Long executionTime = ((Number) metricData.get("executionTime")).longValue();
            Boolean successful = (Boolean) metricData.getOrDefault("successful", true);
            
            metricsService.recordMethodExecution(metricName, executionTime, successful);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Custom metric recorded successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Failed to record custom metric: " + e.getMessage()
            ));
        }
    }
}