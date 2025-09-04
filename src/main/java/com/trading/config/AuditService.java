package com.trading.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for auditing and logging security-related events
 */
@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    // In-memory storage for demonstration (in production, use database)
    private final Map<String, AuditEvent> auditEvents = new ConcurrentHashMap<>();
    
    /**
     * Log a security event
     */
    public void logSecurityEvent(SecurityEventType eventType, String userId, 
                                String details, String ipAddress) {
        try {
            AuditEvent event = new AuditEvent(
                eventType,
                userId,
                details,
                ipAddress,
                LocalDateTime.now()
            );
            
            // Store event
            auditEvents.put(event.getId(), event);
            
            // Log to audit logger
            auditLogger.info("SECURITY_EVENT: {} | User: {} | IP: {} | Details: {} | Time: {}", 
                           eventType, userId, ipAddress, details, event.getTimestamp());
            
            // Check for suspicious activity
            checkSuspiciousActivity(userId, eventType, ipAddress);
            
        } catch (Exception e) {
            logger.error("Failed to log security event", e);
        }
    }
    
    /**
     * Log trading operation
     */
    public void logTradingOperation(String operation, String tradingPair, 
                                  String amount, String price, boolean isVirtual) {
        try {
            String details = String.format("Operation: %s, Pair: %s, Amount: %s, Price: %s, Virtual: %s",
                                         operation, tradingPair, amount, price, isVirtual);
            
            auditLogger.info("TRADING_OPERATION: {}", details);
            
        } catch (Exception e) {
            logger.error("Failed to log trading operation", e);
        }
    }
    
    /**
     * Log configuration change
     */
    public void logConfigurationChange(String userId, String configType, 
                                     String oldValue, String newValue) {
        try {
            String details = String.format("Config: %s, Old: %s, New: %s", 
                                         configType, 
                                         maskSensitiveValue(oldValue), 
                                         maskSensitiveValue(newValue));
            
            logSecurityEvent(SecurityEventType.CONFIGURATION_CHANGE, userId, details, "localhost");
            
        } catch (Exception e) {
            logger.error("Failed to log configuration change", e);
        }
    }
    
    /**
     * Log API access
     */
    public void logApiAccess(String endpoint, String method, String userId, 
                           String ipAddress, int responseCode) {
        try {
            String details = String.format("Endpoint: %s %s, Response: %d", 
                                         method, endpoint, responseCode);
            
            auditLogger.info("API_ACCESS: User: {} | IP: {} | {} | Time: {}", 
                           userId, ipAddress, details, LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Failed to log API access", e);
        }
    }
    
    /**
     * Log authentication events
     */
    public void logAuthenticationEvent(String userId, String ipAddress, 
                                     boolean successful, String reason) {
        try {
            SecurityEventType eventType = successful ? 
                SecurityEventType.LOGIN_SUCCESS : SecurityEventType.LOGIN_FAILURE;
            
            String details = successful ? "Login successful" : "Login failed: " + reason;
            
            logSecurityEvent(eventType, userId, details, ipAddress);
            
        } catch (Exception e) {
            logger.error("Failed to log authentication event", e);
        }
    }
    
    /**
     * Check for suspicious activity patterns
     */
    private void checkSuspiciousActivity(String userId, SecurityEventType eventType, String ipAddress) {
        try {
            // Count recent failed login attempts
            if (eventType == SecurityEventType.LOGIN_FAILURE) {
                long recentFailures = auditEvents.values().stream()
                    .filter(event -> event.getUserId().equals(userId))
                    .filter(event -> event.getEventType() == SecurityEventType.LOGIN_FAILURE)
                    .filter(event -> event.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(15)))
                    .count();
                
                if (recentFailures >= 5) {
                    logSecurityEvent(SecurityEventType.SUSPICIOUS_ACTIVITY, userId, 
                                   "Multiple failed login attempts", ipAddress);
                    logger.warn("SECURITY ALERT: Multiple failed login attempts for user: {}", userId);
                }
            }
            
            // Check for unusual IP addresses
            boolean isNewIpAddress = auditEvents.values().stream()
                .filter(event -> event.getUserId().equals(userId))
                .noneMatch(event -> event.getIpAddress().equals(ipAddress));
            
            if (isNewIpAddress && eventType == SecurityEventType.LOGIN_SUCCESS) {
                logSecurityEvent(SecurityEventType.NEW_IP_ADDRESS, userId, 
                               "Login from new IP address", ipAddress);
                logger.info("INFO: User {} logged in from new IP address: {}", userId, ipAddress);
            }
            
        } catch (Exception e) {
            logger.error("Failed to check suspicious activity", e);
        }
    }
    
    /**
     * Mask sensitive values for logging
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.isEmpty()) {
            return "null";
        }
        
        // Check if it looks like an API key or password
        if (value.length() > 10 && (value.contains("key") || value.contains("pass") || 
                                   value.matches("^[A-Za-z0-9+/=]+$"))) {
            return "****" + value.substring(Math.max(0, value.length() - 4));
        }
        
        return value;
    }
    
    /**
     * Get recent audit events for monitoring
     */
    public java.util.List<AuditEvent> getRecentEvents(int limit) {
        return auditEvents.values().stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Get events by type
     */
    public java.util.List<AuditEvent> getEventsByType(SecurityEventType eventType, int limit) {
        return auditEvents.values().stream()
            .filter(event -> event.getEventType() == eventType)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Security event types
     */
    public enum SecurityEventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        CONFIGURATION_CHANGE,
        API_KEY_CHANGE,
        MODE_SWITCH,
        TRADING_START,
        TRADING_STOP,
        SUSPICIOUS_ACTIVITY,
        NEW_IP_ADDRESS,
        UNAUTHORIZED_ACCESS
    }
    
    /**
     * Audit event data class
     */
    public static class AuditEvent {
        private final String id;
        private final SecurityEventType eventType;
        private final String userId;
        private final String details;
        private final String ipAddress;
        private final LocalDateTime timestamp;
        
        public AuditEvent(SecurityEventType eventType, String userId, String details, 
                         String ipAddress, LocalDateTime timestamp) {
            this.id = java.util.UUID.randomUUID().toString();
            this.eventType = eventType;
            this.userId = userId;
            this.details = details;
            this.ipAddress = ipAddress;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getId() { return id; }
        public SecurityEventType getEventType() { return eventType; }
        public String getUserId() { return userId; }
        public String getDetails() { return details; }
        public String getIpAddress() { return ipAddress; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}