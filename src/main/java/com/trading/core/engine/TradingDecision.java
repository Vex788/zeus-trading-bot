package com.trading.core.engine;

import java.math.BigDecimal;

/**
 * Trading decision data structure
 */
public class TradingDecision {
    
    private final Action action;
    private final BigDecimal amount;
    private final BigDecimal confidence;
    private final String reason;
    private final boolean shouldTrade;
    
    public TradingDecision(Action action, BigDecimal amount, BigDecimal confidence, 
                          String reason, boolean shouldTrade) {
        this.action = action;
        this.amount = amount;
        this.confidence = confidence;
        this.reason = reason;
        this.shouldTrade = shouldTrade;
    }
    
    public static TradingDecision noTrade(String reason) {
        return new TradingDecision(Action.HOLD, BigDecimal.ZERO, BigDecimal.ZERO, reason, false);
    }
    
    public static TradingDecision buy(BigDecimal amount, BigDecimal confidence, String reason) {
        return new TradingDecision(Action.BUY, amount, confidence, reason, true);
    }
    
    public static TradingDecision sell(BigDecimal amount, BigDecimal confidence, String reason) {
        return new TradingDecision(Action.SELL, amount, confidence, reason, true);
    }
    
    // Getters
    public Action getAction() { return action; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getConfidence() { return confidence; }
    public String getReason() { return reason; }
    public boolean shouldTrade() { return shouldTrade; }
    
    public enum Action {
        BUY, SELL, HOLD
    }
    
    @Override
    public String toString() {
        return String.format("TradingDecision{action=%s, amount=%s, confidence=%s, reason='%s', shouldTrade=%s}",
                           action, amount, confidence, reason, shouldTrade);
    }
}