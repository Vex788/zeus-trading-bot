package com.trading.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for collecting and managing application metrics
 */
@Service
public class MetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    
    // Performance metrics
    private final Map<String, MethodMetrics> methodMetrics = new ConcurrentHashMap<>();
    private final Map<String, ApiMetrics> apiMetrics = new ConcurrentHashMap<>();
    private final Map<String, TradingMetrics> tradingMetrics = new ConcurrentHashMap<>();
    private final Map<String, MLMetrics> mlMetrics = new ConcurrentHashMap<>();
    private final Map<String, DatabaseMetrics> databaseMetrics = new ConcurrentHashMap<>();
    
    // Rate limiting
    private final Map<String, RateLimitInfo> rateLimits = new ConcurrentHashMap<>();
    
    // Alerts
    private final Map<String, AlertInfo> alerts = new ConcurrentHashMap<>();
    
    // System metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastHealthCheck = new AtomicReference<>(LocalDateTime.now());
    
    /**
     * Record method execution metrics
     */
    public void recordMethodExecution(String methodName, long executionTime, boolean successful) {
        methodMetrics.computeIfAbsent(methodName, k -> new MethodMetrics())
                    .recordExecution(executionTime, successful);
        
        totalRequests.incrementAndGet();
        if (!successful) {
            totalErrors.incrementAndGet();
        }
    }
    
    /**
     * Record trading operation metrics
     */
    public void recordTradingOperation(String operation, long executionTime, boolean successful) {
        tradingMetrics.computeIfAbsent(operation, k -> new TradingMetrics())
                     .recordOperation(executionTime, successful);
        
        logger.debug("Recorded trading operation: {} - {}ms - {}", 
                    operation, executionTime, successful ? "SUCCESS" : "FAILED");
    }
    
    /**
     * Record API call metrics
     */
    public void recordApiCall(String endpoint, long executionTime, boolean successful) {
        apiMetrics.computeIfAbsent(endpoint, k -> new ApiMetrics())
                 .recordCall(executionTime, successful);
        
        // Update rate limiting info
        rateLimits.computeIfAbsent(endpoint, k -> new RateLimitInfo())
                 .recordCall();
    }
    
    /**
     * Record ML operation metrics
     */
    public void recordMLOperation(String operation, long executionTime, boolean successful) {
        mlMetrics.computeIfAbsent(operation, k -> new MLMetrics())
                .recordOperation(executionTime, successful);
    }
    
    /**
     * Record database operation metrics
     */
    public void recordDatabaseOperation(String operation, long executionTime, boolean successful) {
        databaseMetrics.computeIfAbsent(operation, k -> new DatabaseMetrics())
                      .recordOperation(executionTime, successful);
    }
    
    /**
     * Record API error for pattern analysis
     */
    public void recordApiError(String endpoint, String errorType) {
        apiMetrics.computeIfAbsent(endpoint, k -> new ApiMetrics())
                 .recordError(errorType);
    }
    
    /**
     * Record system alert
     */
    public void recordAlert(String alertType, String message) {
        AlertInfo alert = new AlertInfo(alertType, message, LocalDateTime.now());
        alerts.put(alertType + "_" + System.currentTimeMillis(), alert);
        
        logger.warn("SYSTEM_ALERT: {} - {}", alertType, message);
        
        // Keep only last 100 alerts
        if (alerts.size() > 100) {
            String oldestKey = alerts.keySet().iterator().next();
            alerts.remove(oldestKey);
        }
    }
    
    /**
     * Check if API call is allowed (rate limiting)
     */
    public boolean isApiCallAllowed(String endpoint) {
        RateLimitInfo rateLimitInfo = rateLimits.get(endpoint);
        if (rateLimitInfo == null) {
            return true;
        }
        
        return rateLimitInfo.isCallAllowed();
    }
    
    /**
     * Get system health status
     */
    public SystemHealth getSystemHealth() {
        lastHealthCheck.set(LocalDateTime.now());
        
        double errorRate = totalRequests.get() > 0 ? 
            (double) totalErrors.get() / totalRequests.get() : 0.0;
        
        HealthStatus status;
        if (errorRate < 0.01) {
            status = HealthStatus.HEALTHY;
        } else if (errorRate < 0.05) {
            status = HealthStatus.WARNING;
        } else {
            status = HealthStatus.CRITICAL;
        }
        
        return new SystemHealth(
            status,
            totalRequests.get(),
            totalErrors.get(),
            errorRate,
            getAverageResponseTime(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Get performance summary
     */
    public PerformanceSummary getPerformanceSummary() {
        return new PerformanceSummary(
            methodMetrics.size(),
            getAverageResponseTime(),
            getTotalSuccessfulOperations(),
            getTotalFailedOperations(),
            getTopSlowMethods(5)
        );
    }
    
    /**
     * Get trading metrics summary
     */
    public TradingSummary getTradingSummary() {
        long totalTrades = tradingMetrics.values().stream()
            .mapToLong(TradingMetrics::getTotalOperations)
            .sum();
        
        long successfulTrades = tradingMetrics.values().stream()
            .mapToLong(TradingMetrics::getSuccessfulOperations)
            .sum();
        
        double successRate = totalTrades > 0 ? (double) successfulTrades / totalTrades : 0.0;
        
        return new TradingSummary(
            totalTrades,
            successfulTrades,
            successRate,
            getAverageTradingTime()
        );
    }
    
    // Helper methods
    
    private double getAverageResponseTime() {
        return methodMetrics.values().stream()
            .mapToDouble(MethodMetrics::getAverageExecutionTime)
            .average()
            .orElse(0.0);
    }
    
    private long getTotalSuccessfulOperations() {
        return methodMetrics.values().stream()
            .mapToLong(MethodMetrics::getSuccessfulExecutions)
            .sum();
    }
    
    private long getTotalFailedOperations() {
        return methodMetrics.values().stream()
            .mapToLong(MethodMetrics::getFailedExecutions)
            .sum();
    }
    
    private java.util.List<String> getTopSlowMethods(int limit) {
        return methodMetrics.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue().getAverageExecutionTime(), 
                                             e1.getValue().getAverageExecutionTime()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }
    
    private double getAverageTradingTime() {
        return tradingMetrics.values().stream()
            .mapToDouble(TradingMetrics::getAverageExecutionTime)
            .average()
            .orElse(0.0);
    }
    
    // Metrics data classes
    
    public static class MethodMetrics {
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successfulExecutions = new AtomicLong(0);
        private final AtomicLong failedExecutions = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong maxExecutionTime = new AtomicLong(0);
        private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        
        public void recordExecution(long executionTime, boolean successful) {
            totalExecutions.incrementAndGet();
            totalExecutionTime.addAndGet(executionTime);
            
            if (successful) {
                successfulExecutions.incrementAndGet();
            } else {
                failedExecutions.incrementAndGet();
            }
            
            maxExecutionTime.updateAndGet(max -> Math.max(max, executionTime));
            minExecutionTime.updateAndGet(min -> Math.min(min, executionTime));
        }
        
        public double getAverageExecutionTime() {
            long total = totalExecutions.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
        }
        
        // Getters
        public long getTotalExecutions() { return totalExecutions.get(); }
        public long getSuccessfulExecutions() { return successfulExecutions.get(); }
        public long getFailedExecutions() { return failedExecutions.get(); }
        public long getMaxExecutionTime() { return maxExecutionTime.get(); }
        public long getMinExecutionTime() { 
            long min = minExecutionTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
    }
    
    public static class TradingMetrics extends MethodMetrics {
        public long getTotalOperations() { return getTotalExecutions(); }
        public long getSuccessfulOperations() { return getSuccessfulExecutions(); }
    }
    
    public static class ApiMetrics extends MethodMetrics {
        private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
        
        public void recordError(String errorType) {
            errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0))
                      .incrementAndGet();
        }
        
        public Map<String, Long> getErrorCounts() {
            return errorCounts.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().get()
                ));
        }
    }
    
    public static class MLMetrics extends MethodMetrics {
        // Additional ML-specific metrics can be added here
    }
    
    public static class DatabaseMetrics extends MethodMetrics {
        // Additional database-specific metrics can be added here
    }
    
    public static class RateLimitInfo {
        private final AtomicLong callCount = new AtomicLong(0);
        private final AtomicReference<LocalDateTime> windowStart = new AtomicReference<>(LocalDateTime.now());
        private static final int MAX_CALLS_PER_MINUTE = 60;
        
        public void recordCall() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = windowStart.get();
            
            // Reset window if more than 1 minute has passed
            if (start.isBefore(now.minusMinutes(1))) {
                windowStart.set(now);
                callCount.set(1);
            } else {
                callCount.incrementAndGet();
            }
        }
        
        public boolean isCallAllowed() {
            return callCount.get() < MAX_CALLS_PER_MINUTE;
        }
    }
    
    public static class AlertInfo {
        private final String type;
        private final String message;
        private final LocalDateTime timestamp;
        
        public AlertInfo(String type, String message, LocalDateTime timestamp) {
            this.type = type;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getType() { return type; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public enum HealthStatus {
        HEALTHY, WARNING, CRITICAL
    }
    
    public static class SystemHealth {
        private final HealthStatus status;
        private final long totalRequests;
        private final long totalErrors;
        private final double errorRate;
        private final double averageResponseTime;
        private final LocalDateTime timestamp;
        
        public SystemHealth(HealthStatus status, long totalRequests, long totalErrors,
                          double errorRate, double averageResponseTime, LocalDateTime timestamp) {
            this.status = status;
            this.totalRequests = totalRequests;
            this.totalErrors = totalErrors;
            this.errorRate = errorRate;
            this.averageResponseTime = averageResponseTime;
            this.timestamp = timestamp;
        }
        
        // Getters
        public HealthStatus getStatus() { return status; }
        public long getTotalRequests() { return totalRequests; }
        public long getTotalErrors() { return totalErrors; }
        public double getErrorRate() { return errorRate; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class PerformanceSummary {
        private final int totalMethods;
        private final double averageResponseTime;
        private final long totalSuccessfulOperations;
        private final long totalFailedOperations;
        private final java.util.List<String> topSlowMethods;
        
        public PerformanceSummary(int totalMethods, double averageResponseTime,
                                long totalSuccessfulOperations, long totalFailedOperations,
                                java.util.List<String> topSlowMethods) {
            this.totalMethods = totalMethods;
            this.averageResponseTime = averageResponseTime;
            this.totalSuccessfulOperations = totalSuccessfulOperations;
            this.totalFailedOperations = totalFailedOperations;
            this.topSlowMethods = topSlowMethods;
        }
        
        // Getters
        public int getTotalMethods() { return totalMethods; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public long getTotalSuccessfulOperations() { return totalSuccessfulOperations; }
        public long getTotalFailedOperations() { return totalFailedOperations; }
        public java.util.List<String> getTopSlowMethods() { return topSlowMethods; }
    }
    
    public static class TradingSummary {
        private final long totalTrades;
        private final long successfulTrades;
        private final double successRate;
        private final double averageTradingTime;
        
        public TradingSummary(long totalTrades, long successfulTrades,
                            double successRate, double averageTradingTime) {
            this.totalTrades = totalTrades;
            this.successfulTrades = successfulTrades;
            this.successRate = successRate;
            this.averageTradingTime = averageTradingTime;
        }
        
        // Getters
        public long getTotalTrades() { return totalTrades; }
        public long getSuccessfulTrades() { return successfulTrades; }
        public double getSuccessRate() { return successRate; }
        public double getAverageTradingTime() { return averageTradingTime; }
    }
}