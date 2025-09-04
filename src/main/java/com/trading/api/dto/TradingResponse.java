package com.trading.api.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API response for trading operations
 */
public class TradingResponse {
    
    private boolean success;
    private String message;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
    
    public TradingResponse(boolean success, String message, Map<String, Object> data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}