package com.trading.core.engine;

import java.math.BigDecimal;

/**
 * Order execution result
 */
public class OrderResult {
    
    private final boolean successful;
    private final String orderId;
    private final String message;
    private final BigDecimal confidence;
    
    public OrderResult(boolean successful, String orderId, String message, BigDecimal confidence) {
        this.successful = successful;
        this.orderId = orderId;
        this.message = message;
        this.confidence = confidence;
    }
    
    // Getters
    public boolean isSuccessful() { return successful; }
    public String getOrderId() { return orderId; }
    public String getMessage() { return message; }
    public BigDecimal getConfidence() { return confidence; }
    
    @Override
    public String toString() {
        return String.format("OrderResult{successful=%s, orderId='%s', message='%s', confidence=%s}",
                           successful, orderId, message, confidence);
    }
}