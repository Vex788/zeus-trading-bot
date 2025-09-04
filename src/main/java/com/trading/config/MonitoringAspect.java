package com.trading.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * Monitoring aspect for performance tracking and error handling
 */
@Aspect
@Component
public class MonitoringAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringAspect.class);
    
    @Autowired
    private MetricsService metricsService;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Monitor performance of methods
     */
    @Around("@annotation(MonitorPerformance)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        
        logger.debug("Starting method execution: {}", methodName);
        
        try {
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record performance metrics
            metricsService.recordMethodExecution(methodName, executionTime, true);
            
            if (executionTime > 5000) { // Log slow operations (>5 seconds)
                logger.warn("Slow method execution: {} took {}ms", methodName, executionTime);
            } else {
                logger.debug("Method execution completed: {} in {}ms", methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record failed execution
            metricsService.recordMethodExecution(methodName, executionTime, false);
            
            logger.error("Method execution failed: {} in {}ms - Error: {}", 
                        methodName, executionTime, e.getMessage(), e);
            
            throw e;
        }
    }
    
    /**
     * Monitor trading operations with detailed logging
     */
    @Around("@annotation(MonitorTradingOperation)")
    public Object monitorTradingOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        long startTime = System.currentTimeMillis();
        
        // Log method entry with parameters (mask sensitive data)
        String maskedArgs = maskSensitiveArguments(args);
        logger.info("TRADING_OPERATION_START: {} with args: {}", methodName, maskedArgs);
        
        try {
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record successful trading operation
            metricsService.recordTradingOperation(methodName, executionTime, true);
            
            logger.info("TRADING_OPERATION_SUCCESS: {} completed in {}ms", methodName, executionTime);
            
            // Log to audit service
            auditService.logTradingOperation(
                methodName,
                extractTradingPair(args),
                extractAmount(args),
                extractPrice(args),
                extractIsVirtual(args)
            );
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record failed trading operation
            metricsService.recordTradingOperation(methodName, executionTime, false);
            
            logger.error("TRADING_OPERATION_FAILED: {} failed in {}ms - Error: {}", 
                        methodName, executionTime, e.getMessage());
            
            // Send alert for critical trading failures
            if (isCriticalTradingError(e)) {
                metricsService.recordAlert("CRITICAL_TRADING_ERROR", 
                                         "Trading operation failed: " + methodName + " - " + e.getMessage());
            }
            
            throw e;
        }
    }
    
    /**
     * Monitor API calls with rate limiting and error tracking
     */
    @Around("@annotation(MonitorApiCall)")
    public Object monitorApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String apiEndpoint = extractApiEndpoint(joinPoint);
        long startTime = System.currentTimeMillis();
        
        logger.debug("API_CALL_START: {} to endpoint: {}", methodName, apiEndpoint);
        
        try {
            // Check rate limiting
            if (!metricsService.isApiCallAllowed(apiEndpoint)) {
                throw new RuntimeException("API rate limit exceeded for endpoint: " + apiEndpoint);
            }
            
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record successful API call
            metricsService.recordApiCall(apiEndpoint, executionTime, true);
            
            logger.debug("API_CALL_SUCCESS: {} to {} completed in {}ms", 
                        methodName, apiEndpoint, executionTime);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record failed API call
            metricsService.recordApiCall(apiEndpoint, executionTime, false);
            
            logger.error("API_CALL_FAILED: {} to {} failed in {}ms - Error: {}", 
                        methodName, apiEndpoint, executionTime, e.getMessage());
            
            // Track API error patterns
            metricsService.recordApiError(apiEndpoint, e.getClass().getSimpleName());
            
            throw e;
        }
    }
    
    /**
     * Monitor neural network operations
     */
    @Around("@annotation(MonitorMLOperation)")
    public Object monitorMLOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        long startTime = System.currentTimeMillis();
        
        logger.debug("ML_OPERATION_START: {}", methodName);
        
        try {
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record ML operation metrics
            metricsService.recordMLOperation(methodName, executionTime, true);
            
            logger.info("ML_OPERATION_SUCCESS: {} completed in {}ms", methodName, executionTime);
            
            // Log prediction accuracy if applicable
            if (methodName.contains("predict") && result != null) {
                logger.info("ML_PREDICTION: {} generated prediction", methodName);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record failed ML operation
            metricsService.recordMLOperation(methodName, executionTime, false);
            
            logger.error("ML_OPERATION_FAILED: {} failed in {}ms - Error: {}", 
                        methodName, executionTime, e.getMessage());
            
            // Alert on ML system failures
            metricsService.recordAlert("ML_SYSTEM_ERROR", 
                                     "ML operation failed: " + methodName + " - " + e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * Monitor database operations
     */
    @Around("@annotation(MonitorDatabase)")
    public Object monitorDatabase(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        
        logger.debug("DB_OPERATION_START: {}", methodName);
        
        try {
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record database operation
            metricsService.recordDatabaseOperation(methodName, executionTime, true);
            
            // Log slow database queries
            if (executionTime > 1000) {
                logger.warn("SLOW_DB_QUERY: {} took {}ms", methodName, executionTime);
            }
            
            logger.debug("DB_OPERATION_SUCCESS: {} completed in {}ms", methodName, executionTime);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record failed database operation
            metricsService.recordDatabaseOperation(methodName, executionTime, false);
            
            logger.error("DB_OPERATION_FAILED: {} failed in {}ms - Error: {}", 
                        methodName, executionTime, e.getMessage());
            
            // Alert on database connectivity issues
            if (isDatabaseConnectivityError(e)) {
                metricsService.recordAlert("DATABASE_CONNECTIVITY_ERROR", 
                                         "Database operation failed: " + methodName + " - " + e.getMessage());
            }
            
            throw e;
        }
    }
    
    // Helper methods
    
    private String maskSensitiveArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        return Arrays.stream(args)
            .map(arg -> {
                if (arg == null) return "null";
                String str = arg.toString();
                // Mask potential sensitive data
                if (str.length() > 10 && (str.contains("key") || str.contains("secret") || 
                                         str.matches("^[A-Za-z0-9+/=]+$"))) {
                    return "****" + str.substring(Math.max(0, str.length() - 4));
                }
                return str;
            })
            .toArray()
            .toString();
    }
    
    private String extractTradingPair(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String && ((String) arg).contains("-")) {
                return (String) arg;
            }
        }
        return "UNKNOWN";
    }
    
    private String extractAmount(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof java.math.BigDecimal) {
                return arg.toString();
            }
        }
        return "0";
    }
    
    private String extractPrice(Object[] args) {
        // Similar logic to extract price from arguments
        return "0";
    }
    
    private boolean extractIsVirtual(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Boolean) {
                return (Boolean) arg;
            }
        }
        return true; // Default to virtual for safety
    }
    
    private String extractApiEndpoint(ProceedingJoinPoint joinPoint) {
        // Extract API endpoint from method name or annotations
        String methodName = joinPoint.getSignature().getName();
        if (methodName.contains("KuCoin") || methodName.contains("Api")) {
            return "kucoin-api";
        }
        return "unknown-api";
    }
    
    private boolean isCriticalTradingError(Exception e) {
        return e.getMessage().contains("insufficient") || 
               e.getMessage().contains("network") ||
               e.getMessage().contains("timeout") ||
               e.getMessage().contains("unauthorized");
    }
    
    private boolean isDatabaseConnectivityError(Exception e) {
        return e.getMessage().contains("connection") ||
               e.getMessage().contains("timeout") ||
               e.getMessage().contains("database");
    }
}

// Monitoring annotations

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MonitorPerformance {
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MonitorTradingOperation {
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MonitorApiCall {
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MonitorMLOperation {
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MonitorDatabase {
}