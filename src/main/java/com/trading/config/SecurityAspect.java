package com.trading.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Security aspect for monitoring and auditing sensitive operations
 */
@Aspect
@Component
public class SecurityAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityAspect.class);
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Monitor trading operations
     */
    @Around("@annotation(MonitorTrade)")
    public Object monitorTrading(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String operation = joinPoint.getSignature().getName();
        String userId = getCurrentUserId();
        String ipAddress = getCurrentIpAddress();
        
        try {
            logger.debug("Starting trading operation: {} by user: {}", operation, userId);
            
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log successful operation
            auditService.logApiAccess(
                "/api/trading/" + operation,
                "POST",
                userId,
                ipAddress,
                200
            );
            
            logger.info("Trading operation completed: {} in {}ms by user: {}", 
                       operation, executionTime, userId);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log failed operation
            auditService.logApiAccess(
                "/api/trading/" + operation,
                "POST",
                userId,
                ipAddress,
                500
            );
            
            auditService.logSecurityEvent(
                AuditService.SecurityEventType.UNAUTHORIZED_ACCESS,
                userId,
                "Trading operation failed: " + operation + " - " + e.getMessage(),
                ipAddress
            );
            
            logger.error("Trading operation failed: {} in {}ms by user: {} - Error: {}", 
                        operation, executionTime, userId, e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * Monitor configuration changes
     */
    @Around("@annotation(MonitorConfig)")
    public Object monitorConfiguration(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().getName();
        String userId = getCurrentUserId();
        String ipAddress = getCurrentIpAddress();
        
        try {
            logger.debug("Starting configuration operation: {} by user: {}", operation, userId);
            
            Object result = joinPoint.proceed();
            
            // Log configuration change
            auditService.logSecurityEvent(
                AuditService.SecurityEventType.CONFIGURATION_CHANGE,
                userId,
                "Configuration operation: " + operation,
                ipAddress
            );
            
            logger.info("Configuration operation completed: {} by user: {}", operation, userId);
            
            return result;
            
        } catch (Exception e) {
            auditService.logSecurityEvent(
                AuditService.SecurityEventType.UNAUTHORIZED_ACCESS,
                userId,
                "Configuration operation failed: " + operation + " - " + e.getMessage(),
                ipAddress
            );
            
            logger.error("Configuration operation failed: {} by user: {} - Error: {}", 
                        operation, userId, e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * Monitor sensitive data access
     */
    @Around("@annotation(MonitorSensitive)")
    public Object monitorSensitiveAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().getName();
        String userId = getCurrentUserId();
        String ipAddress = getCurrentIpAddress();
        
        try {
            logger.debug("Accessing sensitive data: {} by user: {}", operation, userId);
            
            Object result = joinPoint.proceed();
            
            // Log sensitive data access
            auditService.logSecurityEvent(
                AuditService.SecurityEventType.API_KEY_CHANGE,
                userId,
                "Sensitive data access: " + operation,
                ipAddress
            );
            
            return result;
            
        } catch (Exception e) {
            auditService.logSecurityEvent(
                AuditService.SecurityEventType.UNAUTHORIZED_ACCESS,
                userId,
                "Sensitive data access failed: " + operation + " - " + e.getMessage(),
                ipAddress
            );
            
            logger.error("Sensitive data access failed: {} by user: {} - Error: {}", 
                        operation, userId, e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * Get current authenticated user ID
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
            return "anonymous";
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Get current request IP address
     */
    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("X-Real-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            
            return ipAddress;
        } catch (Exception e) {
            return "unknown";
        }
    }
}

/**
 * Annotation for monitoring trading operations
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MonitorTrade {
}

/**
 * Annotation for monitoring configuration changes
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MonitorConfig {
}

/**
 * Annotation for monitoring sensitive data access
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MonitorSensitive {
}