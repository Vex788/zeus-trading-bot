package com.trading.core.strategy;

import com.trading.config.TradingBotConfig;
import com.trading.core.engine.TradingDecision;
import com.trading.domain.repositories.TradingHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Risk management component for trading operations
 */
@Component
public class RiskManagement {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskManagement.class);
    
    @Autowired
    private TradingBotConfig config;
    
    @Autowired
    private TradingHistoryRepository tradingHistoryRepository;
    
    /**
     * Check if a trade is allowed based on risk parameters
     */
    public boolean isTradeAllowed(TradingDecision decision, BigDecimal currentBalance) {
        try {
            // Check daily loss limit
            if (!isDailyLossWithinLimit(currentBalance)) {
                logger.warn("Trade rejected: Daily loss limit exceeded");
                return false;
            }
            
            // Check position size limit
            if (!isPositionSizeAllowed(decision.getAmount(), currentBalance)) {
                logger.warn("Trade rejected: Position size too large");
                return false;
            }
            
            // Check minimum confidence for high-risk trades
            if (!isConfidenceAcceptable(decision)) {
                logger.warn("Trade rejected: Confidence too low for trade size");
                return false;
            }
            
            // Check if we're not overtrading
            if (!isTradingFrequencyAcceptable()) {
                logger.warn("Trade rejected: Trading too frequently");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error in risk management check", e);
            return false; // Fail safe
        }
    }
    
    /**
     * Calculate appropriate position size based on risk parameters
     */
    public BigDecimal calculatePositionSize(BigDecimal requestedAmount, BigDecimal currentBalance) {
        try {
            // Apply maximum position size percentage
            BigDecimal maxPositionValue = currentBalance
                .multiply(BigDecimal.valueOf(config.getRisk().getMaxPositionSizePercent() / 100.0));
            
            // Calculate maximum allowed amount (simplified - assumes 1:1 price ratio)
            BigDecimal maxAllowedAmount = maxPositionValue;
            
            // Return the smaller of requested amount or maximum allowed
            BigDecimal finalAmount = requestedAmount.min(maxAllowedAmount);
            
            // Ensure minimum trade size
            BigDecimal minTradeSize = BigDecimal.valueOf(0.001);
            if (finalAmount.compareTo(minTradeSize) < 0) {
                return BigDecimal.ZERO; // Too small to trade
            }
            
            logger.debug("Position size calculated: requested={}, max={}, final={}", 
                        requestedAmount, maxAllowedAmount, finalAmount);
            
            return finalAmount.setScale(8, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            logger.error("Error calculating position size", e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculate stop-loss price
     */
    public BigDecimal calculateStopLoss(BigDecimal entryPrice, TradingDecision.Action action) {
        BigDecimal stopLossPercent = BigDecimal.valueOf(config.getRisk().getStopLossPercent() / 100.0);
        
        if (action == TradingDecision.Action.BUY) {
            // For buy orders, stop-loss is below entry price
            return entryPrice.multiply(BigDecimal.ONE.subtract(stopLossPercent));
        } else {
            // For sell orders, stop-loss is above entry price
            return entryPrice.multiply(BigDecimal.ONE.add(stopLossPercent));
        }
    }
    
    /**
     * Calculate take-profit price
     */
    public BigDecimal calculateTakeProfit(BigDecimal entryPrice, TradingDecision.Action action) {
        BigDecimal takeProfitPercent = BigDecimal.valueOf(config.getRisk().getTakeProfitPercent() / 100.0);
        
        if (action == TradingDecision.Action.BUY) {
            // For buy orders, take-profit is above entry price
            return entryPrice.multiply(BigDecimal.ONE.add(takeProfitPercent));
        } else {
            // For sell orders, take-profit is below entry price
            return entryPrice.multiply(BigDecimal.ONE.subtract(takeProfitPercent));
        }
    }
    
    /**
     * Check if daily loss is within acceptable limits
     */
    private boolean isDailyLossWithinLimit(BigDecimal currentBalance) {
        try {
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            BigDecimal dailyProfitLoss = tradingHistoryRepository.calculateDailyProfitLoss(startOfDay, true);
            
            if (dailyProfitLoss == null) {
                return true; // No trades today
            }
            
            BigDecimal maxDailyLoss = currentBalance
                .multiply(BigDecimal.valueOf(config.getRisk().getMaxDailyLossPercent() / 100.0))
                .negate(); // Make it negative for loss
            
            boolean withinLimit = dailyProfitLoss.compareTo(maxDailyLoss) >= 0;
            
            if (!withinLimit) {
                logger.warn("Daily loss limit exceeded: current={}, limit={}", dailyProfitLoss, maxDailyLoss);
            }
            
            return withinLimit;
            
        } catch (Exception e) {
            logger.error("Error checking daily loss limit", e);
            return true; // Fail safe - allow trade
        }
    }
    
    /**
     * Check if position size is within allowed limits
     */
    private boolean isPositionSizeAllowed(BigDecimal amount, BigDecimal currentBalance) {
        BigDecimal maxPositionValue = currentBalance
            .multiply(BigDecimal.valueOf(config.getRisk().getMaxPositionSizePercent() / 100.0));
        
        // Simplified check - assumes amount is in base currency
        return amount.compareTo(maxPositionValue) <= 0;
    }
    
    /**
     * Check if confidence level is acceptable for the trade
     */
    private boolean isConfidenceAcceptable(TradingDecision decision) {
        // Higher confidence required for larger trades
        BigDecimal minConfidence = BigDecimal.valueOf(0.6);
        
        // Increase confidence requirement for larger positions
        if (decision.getAmount().compareTo(BigDecimal.valueOf(0.1)) > 0) {
            minConfidence = BigDecimal.valueOf(0.7);
        }
        
        if (decision.getAmount().compareTo(BigDecimal.valueOf(0.5)) > 0) {
            minConfidence = BigDecimal.valueOf(0.8);
        }
        
        return decision.getConfidence().compareTo(minConfidence) >= 0;
    }
    
    /**
     * Check if trading frequency is acceptable (prevent overtrading)
     */
    private boolean isTradingFrequencyAcceptable() {
        try {
            // Check if we've made too many trades in the last hour
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long recentTradesCount = tradingHistoryRepository.findRecentTrades(oneHourAgo).size();
            
            int maxTradesPerHour = 10; // Configurable limit
            
            if (recentTradesCount >= maxTradesPerHour) {
                logger.warn("Trading frequency limit exceeded: {} trades in last hour", recentTradesCount);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking trading frequency", e);
            return true; // Fail safe - allow trade
        }
    }
    
    /**
     * Calculate risk score for a trade
     */
    public BigDecimal calculateRiskScore(TradingDecision decision, BigDecimal currentPrice, BigDecimal currentBalance) {
        try {
            BigDecimal riskScore = BigDecimal.ZERO;
            
            // Position size risk (0-40 points)
            BigDecimal positionRisk = decision.getAmount()
                .divide(currentBalance, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(40));
            riskScore = riskScore.add(positionRisk);
            
            // Confidence risk (0-30 points, inverse of confidence)
            BigDecimal confidenceRisk = BigDecimal.ONE.subtract(decision.getConfidence())
                .multiply(BigDecimal.valueOf(30));
            riskScore = riskScore.add(confidenceRisk);
            
            // Market volatility risk (0-30 points) - simplified
            BigDecimal volatilityRisk = BigDecimal.valueOf(15); // Default medium risk
            riskScore = riskScore.add(volatilityRisk);
            
            return riskScore.setScale(2, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            logger.error("Error calculating risk score", e);
            return BigDecimal.valueOf(100); // Maximum risk on error
        }
    }
    
    /**
     * Get risk management status
     */
    public RiskStatus getRiskStatus(BigDecimal currentBalance) {
        try {
            BigDecimal dailyProfitLoss = tradingHistoryRepository.calculateDailyProfitLoss(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0), true);
            
            if (dailyProfitLoss == null) {
                dailyProfitLoss = BigDecimal.ZERO;
            }
            
            BigDecimal dailyProfitLossPercent = dailyProfitLoss
                .divide(currentBalance, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            
            return new RiskStatus(
                isDailyLossWithinLimit(currentBalance),
                dailyProfitLoss,
                dailyProfitLossPercent,
                isTradingFrequencyAcceptable()
            );
            
        } catch (Exception e) {
            logger.error("Error getting risk status", e);
            return new RiskStatus(false, BigDecimal.ZERO, BigDecimal.ZERO, false);
        }
    }
    
    /**
     * Risk status data class
     */
    public static class RiskStatus {
        private final boolean dailyLossWithinLimit;
        private final BigDecimal dailyProfitLoss;
        private final BigDecimal dailyProfitLossPercent;
        private final boolean tradingFrequencyOk;
        
        public RiskStatus(boolean dailyLossWithinLimit, BigDecimal dailyProfitLoss, 
                         BigDecimal dailyProfitLossPercent, boolean tradingFrequencyOk) {
            this.dailyLossWithinLimit = dailyLossWithinLimit;
            this.dailyProfitLoss = dailyProfitLoss;
            this.dailyProfitLossPercent = dailyProfitLossPercent;
            this.tradingFrequencyOk = tradingFrequencyOk;
        }
        
        // Getters
        public boolean isDailyLossWithinLimit() { return dailyLossWithinLimit; }
        public BigDecimal getDailyProfitLoss() { return dailyProfitLoss; }
        public BigDecimal getDailyProfitLossPercent() { return dailyProfitLossPercent; }
        public boolean isTradingFrequencyOk() { return tradingFrequencyOk; }
        
        public boolean isOverallRiskAcceptable() {
            return dailyLossWithinLimit && tradingFrequencyOk;
        }
    }
}