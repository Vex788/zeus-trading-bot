package com.trading.core.strategy;

import com.trading.config.TradingBotConfig;
import com.trading.core.engine.TradingDecision;
import com.trading.domain.repositories.TradingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RiskManagement
 */
@ExtendWith(MockitoExtension.class)
class RiskManagementTest {
    
    @Mock
    private TradingBotConfig config;
    
    @Mock
    private TradingBotConfig.RiskConfig riskConfig;
    
    @Mock
    private TradingHistoryRepository tradingHistoryRepository;
    
    @InjectMocks
    private RiskManagement riskManagement;
    
    @BeforeEach
    void setUp() {
        when(config.getRisk()).thenReturn(riskConfig);
        when(riskConfig.getMaxPositionSizePercent()).thenReturn(10.0);
        when(riskConfig.getStopLossPercent()).thenReturn(5.0);
        when(riskConfig.getTakeProfitPercent()).thenReturn(15.0);
        when(riskConfig.getMaxDailyLossPercent()).thenReturn(20.0);
    }
    
    @Test
    void testTradeAllowedWithValidParameters() {
        // Given
        TradingDecision decision = TradingDecision.buy(
            BigDecimal.valueOf(0.1), 
            BigDecimal.valueOf(0.8), 
            "High confidence buy signal"
        );
        BigDecimal currentBalance = BigDecimal.valueOf(1000);
        
        when(tradingHistoryRepository.calculateDailyProfitLoss(any(LocalDateTime.class), eq(true)))
            .thenReturn(BigDecimal.valueOf(-5)); // Small loss within limits
        when(tradingHistoryRepository.findRecentTrades(any(LocalDateTime.class)))
            .thenReturn(java.util.List.of()); // No recent trades
        
        // When
        boolean allowed = riskManagement.isTradeAllowed(decision, currentBalance);
        
        // Then
        assertTrue(allowed);
    }
    
    @Test
    void testTradeRejectedDueToDailyLossLimit() {
        // Given
        TradingDecision decision = TradingDecision.buy(
            BigDecimal.valueOf(0.1), 
            BigDecimal.valueOf(0.8), 
            "Buy signal"
        );
        BigDecimal currentBalance = BigDecimal.valueOf(1000);
        
        // Daily loss exceeds 20% limit
        when(tradingHistoryRepository.calculateDailyProfitLoss(any(LocalDateTime.class), eq(true)))
            .thenReturn(BigDecimal.valueOf(-250)); // 25% loss
        
        // When
        boolean allowed = riskManagement.isTradeAllowed(decision, currentBalance);
        
        // Then
        assertFalse(allowed);
    }
    
    @Test
    void testCalculatePositionSizeWithinLimits() {
        // Given
        BigDecimal requestedAmount = BigDecimal.valueOf(50);
        BigDecimal currentBalance = BigDecimal.valueOf(1000);
        
        // When
        BigDecimal positionSize = riskManagement.calculatePositionSize(requestedAmount, currentBalance);
        
        // Then
        assertEquals(requestedAmount, positionSize); // Should return requested amount as it's within 10% limit
    }
    
    @Test
    void testCalculatePositionSizeExceedsLimits() {
        // Given
        BigDecimal requestedAmount = BigDecimal.valueOf(200); // 20% of balance
        BigDecimal currentBalance = BigDecimal.valueOf(1000);
        
        // When
        BigDecimal positionSize = riskManagement.calculatePositionSize(requestedAmount, currentBalance);
        
        // Then
        assertEquals(BigDecimal.valueOf(100), positionSize); // Should be capped at 10% (100)
    }
    
    @Test
    void testCalculatePositionSizeTooSmall() {
        // Given
        BigDecimal requestedAmount = BigDecimal.valueOf(0.0005); // Very small amount
        BigDecimal currentBalance = BigDecimal.valueOf(1000);
        
        // When
        BigDecimal positionSize = riskManagement.calculatePositionSize(requestedAmount, currentBalance);
        
        // Then
        assertEquals(BigDecimal.ZERO, positionSize); // Should return zero for amounts too small to trade
    }
    
    @Test
    void testCalculateStopLossForBuyOrder() {
        // Given
        BigDecimal entryPrice = BigDecimal.valueOf(100);
        TradingDecision.Action action = TradingDecision.Action.BUY;
        
        // When
        BigDecimal stopLoss = riskManagement.calculateStopLoss(entryPrice, action);
        
        // Then
        assertEquals(BigDecimal.valueOf(95), stopLoss); // 5% below entry price
    }
    
    @Test
    void testCalculateStopLossForSellOrder() {
        // Given
        BigDecimal entryPrice = BigDecimal.valueOf(100);
        TradingDecision.Action action = TradingDecision.Action.SELL;
        
        // When
        BigDecimal stopLoss = riskManagement.calculateStopLoss(entryPrice, action);
        
        // Then
        assertEquals(BigDecimal.valueOf(105), stopLoss); // 5% above entry price
    }
    
    @Test
    void testCalculateTakeProfitForBuyOrder() {
        // Given
        BigDecimal entryPrice = BigDecimal.valueOf(100);
        TradingDecision.Action action = TradingDecision.Action.BUY;
        
        // When
        BigDecimal takeProfit = riskManagement.calculateTakeProfit(entryPrice, action);
        
        // Then
        assertEquals(BigDecimal.valueOf(115), takeProfit); // 15% above entry price
    }
    
    @Test
    void testCalculateTakeProfitForSellOrder() {
        // Given
        BigDecimal entryPrice = BigDecimal.valueOf(100);
        TradingDecision.Action action = TradingDecision.Action.SELL;
        
        // When
        BigDecimal takeProfit = riskManagement.calculateTakeProfit(entryPrice, action);
        
        // Then
        assertEquals(BigDecimal.valueOf(85), takeProfit); // 15% below entry price
    }
    
    @Test
    void testCalculateRiskScore() {
        // Given
        TradingDecision decision = TradingDecision.buy(
            BigDecimal.valueOf(100), 
            BigDecimal.valueOf(0.7), 
            "Medium confidence trade"
        );
        BigDecimal currentPrice = BigDecimal.valueOf(50000);
        BigDecimal currentBalance = BigDecimal.valueOf(1000);
        
        // When
        BigDecimal riskScore = riskManagement.calculateRiskScore(decision, currentPrice, currentBalance);
        
        // Then
        assertNotNull(riskScore);
        assertTrue(riskScore.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(riskScore.compareTo(BigDecimal.valueOf(100)) <= 0);
    }
    
    @Test
    void testGetRiskStatusWithinLimits() {
        // Given
        BigDecimal currentBalance = BigDecimal.valueOf(1000);
        when(tradingHistoryRepository.calculateDailyProfitLoss(any(LocalDateTime.class), eq(true)))
            .thenReturn(BigDecimal.valueOf(-50)); // 5% loss, within limits
        when(tradingHistoryRepository.findRecentTrades(any(LocalDateTime.class)))
            .thenReturn(java.util.List.of()); // No recent trades
        
        // When
        RiskManagement.RiskStatus status = riskManagement.getRiskStatus(currentBalance);
        
        // Then
        assertTrue(status.isDailyLossWithinLimit());
        assertTrue(status.isTradingFrequencyOk());
        assertTrue(status.isOverallRiskAcceptable());
    }
    
    @Test
    void testGetRiskStatusExceedsLimits() {
        // Given
        BigDecimal currentBalance = BigDecimal.valueOf(1000);
        when(tradingHistoryRepository.calculateDailyProfitLoss(any(LocalDateTime.class), eq(true)))
            .thenReturn(BigDecimal.valueOf(-250)); // 25% loss, exceeds limits
        
        // When
        RiskManagement.RiskStatus status = riskManagement.getRiskStatus(currentBalance);
        
        // Then
        assertFalse(status.isDailyLossWithinLimit());
        assertFalse(status.isOverallRiskAcceptable());
    }
}