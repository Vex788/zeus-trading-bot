package com.trading.core.strategy;

import com.trading.config.TradingBotConfig;
import com.trading.core.engine.TradingDecision;
import com.trading.core.ml.NeuralNetwork;
import com.trading.integration.kucoin.MarketDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Neural network-based trading strategy
 */
@Component
public class NeuralNetworkStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(NeuralNetworkStrategy.class);
    
    @Autowired
    private TradingBotConfig config;
    
    @Autowired
    private MarketDataFetcher marketDataFetcher;
    
    private static final BigDecimal MIN_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.6);
    private static final BigDecimal HIGH_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.8);
    
    /**
     * Make trading decision based on neural network prediction
     */
    public TradingDecision makeDecision(String tradingPair, NeuralNetwork.TradingPrediction prediction, 
                                      BigDecimal currentPrice) {
        try {
            // Check confidence threshold
            BigDecimal confidence = BigDecimal.valueOf(prediction.getConfidence());
            if (confidence.compareTo(MIN_CONFIDENCE_THRESHOLD) < 0) {
                return TradingDecision.noTrade("Low confidence: " + confidence);
            }
            
            // Get market conditions
            MarketDataFetcher.MarketData marketData = marketDataFetcher.getMarketData(tradingPair);
            if (marketData == null) {
                return TradingDecision.noTrade("No market data available");
            }
            
            // Calculate position size based on prediction and confidence
            BigDecimal baseAmount = calculateBaseAmount(tradingPair, currentPrice);
            BigDecimal positionMultiplier = BigDecimal.valueOf(prediction.getPositionSize());
            BigDecimal confidenceMultiplier = confidence.divide(BigDecimal.valueOf(1.0), 4, RoundingMode.HALF_UP);
            
            BigDecimal amount = baseAmount
                .multiply(positionMultiplier)
                .multiply(confidenceMultiplier)
                .setScale(8, RoundingMode.HALF_UP);
            
            // Make decision based on prediction direction
            if (prediction.isPredictingUp()) {
                return makeBuyDecision(tradingPair, amount, confidence, prediction);
            } else if (prediction.isPredictingDown()) {
                return makeSellDecision(tradingPair, amount, confidence, prediction);
            } else {
                return TradingDecision.noTrade("Neutral prediction");
            }
            
        } catch (Exception e) {
            logger.error("Error making trading decision for {}", tradingPair, e);
            return TradingDecision.noTrade("Strategy error: " + e.getMessage());
        }
    }
    
    /**
     * Make buy decision
     */
    private TradingDecision makeBuyDecision(String tradingPair, BigDecimal amount, 
                                          BigDecimal confidence, NeuralNetwork.TradingPrediction prediction) {
        
        // Additional buy conditions
        if (!isBuyConditionsMet(tradingPair, prediction)) {
            return TradingDecision.noTrade("Buy conditions not met");
        }
        
        String reason = String.format("ML predicts UP with %.2f%% confidence, position size: %.4f",
                                    confidence.multiply(BigDecimal.valueOf(100)).doubleValue(),
                                    prediction.getPositionSize());
        
        return TradingDecision.buy(amount, confidence, reason);
    }
    
    /**
     * Make sell decision
     */
    private TradingDecision makeSellDecision(String tradingPair, BigDecimal amount, 
                                           BigDecimal confidence, NeuralNetwork.TradingPrediction prediction) {
        
        // Additional sell conditions
        if (!isSellConditionsMet(tradingPair, prediction)) {
            return TradingDecision.noTrade("Sell conditions not met");
        }
        
        String reason = String.format("ML predicts DOWN with %.2f%% confidence, position size: %.4f",
                                    confidence.multiply(BigDecimal.valueOf(100)).doubleValue(),
                                    prediction.getPositionSize());
        
        return TradingDecision.sell(amount, confidence, reason);
    }
    
    /**
     * Check if buy conditions are met
     */
    private boolean isBuyConditionsMet(String tradingPair, NeuralNetwork.TradingPrediction prediction) {
        try {
            // Check if we have enough confidence for buying
            if (prediction.getConfidence() < HIGH_CONFIDENCE_THRESHOLD.doubleValue()) {
                logger.debug("Buy confidence too low for {}: {}", tradingPair, prediction.getConfidence());
                return false;
            }
            
            // Check market conditions (simplified)
            MarketDataFetcher.MarketData marketData = marketDataFetcher.getMarketData(tradingPair);
            if (marketData != null) {
                // Don't buy if spread is too wide
                BigDecimal spread = marketData.getAskPrice().subtract(marketData.getBidPrice());
                BigDecimal spreadPercent = spread.divide(marketData.getLastPrice(), 4, RoundingMode.HALF_UP);
                
                if (spreadPercent.compareTo(BigDecimal.valueOf(0.01)) > 0) { // 1% spread threshold
                    logger.debug("Spread too wide for {}: {}%", tradingPair, spreadPercent.multiply(BigDecimal.valueOf(100)));
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking buy conditions for {}", tradingPair, e);
            return false;
        }
    }
    
    /**
     * Check if sell conditions are met
     */
    private boolean isSellConditionsMet(String tradingPair, NeuralNetwork.TradingPrediction prediction) {
        try {
            // Check if we have enough confidence for selling
            if (prediction.getConfidence() < MIN_CONFIDENCE_THRESHOLD.doubleValue()) {
                logger.debug("Sell confidence too low for {}: {}", tradingPair, prediction.getConfidence());
                return false;
            }
            
            // Additional sell conditions can be added here
            // For example: check if we actually have the asset to sell
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking sell conditions for {}", tradingPair, e);
            return false;
        }
    }
    
    /**
     * Calculate base amount for trading
     */
    private BigDecimal calculateBaseAmount(String tradingPair, BigDecimal currentPrice) {
        // Base amount calculation (simplified)
        // In production, this would consider available balance, risk parameters, etc.
        
        BigDecimal maxPositionValue = BigDecimal.valueOf(50); // $50 max position
        BigDecimal baseAmount = maxPositionValue.divide(currentPrice, 8, RoundingMode.HALF_UP);
        
        // Apply maximum position size from risk management
        BigDecimal maxPositionPercent = BigDecimal.valueOf(config.getRisk().getMaxPositionSizePercent() / 100.0);
        BigDecimal maxAllowedAmount = baseAmount.multiply(maxPositionPercent);
        
        return baseAmount.min(maxAllowedAmount);
    }
    
    /**
     * Check if trading is allowed for the pair
     */
    public boolean isTradingAllowed(String tradingPair) {
        return config.getTradingPairs().contains(tradingPair);
    }
    
    /**
     * Get strategy name
     */
    public String getStrategyName() {
        return "NeuralNetworkStrategy";
    }
    
    /**
     * Get strategy parameters
     */
    public String getStrategyParameters() {
        return String.format("MinConfidence: %.2f, HighConfidence: %.2f", 
                           MIN_CONFIDENCE_THRESHOLD.doubleValue(),
                           HIGH_CONFIDENCE_THRESHOLD.doubleValue());
    }
}