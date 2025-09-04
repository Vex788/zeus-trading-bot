package com.trading.api.controllers;

import com.trading.api.dto.TradingResponse;
import com.trading.core.engine.TradingEngine;
import com.trading.core.strategy.RiskManagement;
import com.trading.domain.entities.BotConfig;
import com.trading.domain.entities.Order;
import com.trading.domain.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TradingController
 */
@ExtendWith(MockitoExtension.class)
class TradingControllerTest {
    
    @Mock
    private TradingEngine tradingEngine;
    
    @Mock
    private RiskManagement riskManagement;
    
    @Mock
    private OrderRepository orderRepository;
    
    @InjectMocks
    private TradingController tradingController;
    
    @BeforeEach
    void setUp() {
        when(tradingEngine.getCurrentMode()).thenReturn(BotConfig.TradingMode.SHADOW);
    }
    
    @Test
    void testStartTradingWhenNotRunning() {
        // Given
        when(tradingEngine.isRunning()).thenReturn(false);
        
        // When
        ResponseEntity<TradingResponse> response = tradingController.startTrading(null);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Trading bot started successfully", response.getBody().getMessage());
        verify(tradingEngine).start();
    }
    
    @Test
    void testStartTradingWhenAlreadyRunning() {
        // Given
        when(tradingEngine.isRunning()).thenReturn(true);
        
        // When
        ResponseEntity<TradingResponse> response = tradingController.startTrading(null);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Trading bot is already running", response.getBody().getMessage());
        verify(tradingEngine, never()).start();
    }
    
    @Test
    void testStopTradingWhenRunning() {
        // Given
        when(tradingEngine.isRunning()).thenReturn(true);
        
        // When
        ResponseEntity<TradingResponse> response = tradingController.stopTrading();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Trading bot stopped successfully", response.getBody().getMessage());
        verify(tradingEngine).stop();
    }
    
    @Test
    void testStopTradingWhenNotRunning() {
        // Given
        when(tradingEngine.isRunning()).thenReturn(false);
        
        // When
        ResponseEntity<TradingResponse> response = tradingController.stopTrading();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Trading bot is not running", response.getBody().getMessage());
        verify(tradingEngine, never()).stop();
    }
    
    @Test
    void testPauseTradingWhenRunningAndNotPaused() {
        // Given
        when(tradingEngine.isRunning()).thenReturn(true);
        when(tradingEngine.isPaused()).thenReturn(false);
        
        // When
        ResponseEntity<TradingResponse> response = tradingController.pauseTrading();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Trading bot paused successfully", response.getBody().getMessage());
        verify(tradingEngine).pause();
    }
    
    @Test
    void testPauseTradingWhenNotRunning() {
        // Given
        when(tradingEngine.isRunning()).thenReturn(false);
        
        // When
        ResponseEntity<TradingResponse> response = tradingController.pauseTrading();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        verify(tradingEngine, never()).pause();
    }
    
    @Test
    void testResumeTradingWhenPaused() {
        // Given
        when(tradingEngine.isRunning()).thenReturn(true);
        when(tradingEngine.isPaused()).thenReturn(true);
        
        // When
        ResponseEntity<TradingResponse> response = tradingController.resumeTrading();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Trading bot resumed successfully", response.getBody().getMessage());
        verify(tradingEngine).resume();
    }
    
    @Test
    void testSwitchModeToProduction() {
        // Given
        when(tradingEngine.getCurrentMode()).thenReturn(BotConfig.TradingMode.SHADOW);
        
        // When
        ResponseEntity<TradingResponse> response = tradingController.switchMode("PRODUCTION");
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Trading mode switched successfully", response.getBody().getMessage());
        verify(tradingEngine).switchMode(BotConfig.TradingMode.PRODUCTION);
    }
    
    @Test
    void testSwitchModeToSameMode() {
        // Given
        when(tradingEngine.getCurrentMode()).thenReturn(BotConfig.TradingMode.SHADOW);
        
        // When
        ResponseEntity<TradingResponse> response = tradingController.switchMode("SHADOW");
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Already in SHADOW mode", response.getBody().getMessage());
        verify(tradingEngine, never()).switchMode(any());
    }
    
    @Test
    void testSwitchModeWithInvalidMode() {
        // When
        ResponseEntity<TradingResponse> response = tradingController.switchMode("INVALID");
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Invalid mode"));
        verify(tradingEngine, never()).switchMode(any());
    }
    
    @Test
    void testGetTradingStatus() {
        // Given
        when(tradingEngine.isRunning()).thenReturn(true);
        when(tradingEngine.isPaused()).thenReturn(false);
        when(tradingEngine.getCurrentMode()).thenReturn(BotConfig.TradingMode.SHADOW);
        
        // When
        ResponseEntity<Map<String, Object>> response = tradingController.getTradingStatus();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertTrue((Boolean) body.get("isRunning"));
        assertFalse((Boolean) body.get("isPaused"));
        assertEquals("SHADOW", body.get("mode"));
        assertEquals("RUNNING", body.get("status"));
    }
    
    @Test
    void testGetActiveOrders() {
        // Given
        List<Order> activeOrders = List.of(
            new Order("order1", "BTC-USDT", Order.OrderSide.BUY, 
                     BigDecimal.valueOf(50000), BigDecimal.valueOf(0.1), true),
            new Order("order2", "ETH-USDT", Order.OrderSide.SELL, 
                     BigDecimal.valueOf(3000), BigDecimal.valueOf(1.0), true)
        );
        when(orderRepository.findActiveOrders()).thenReturn(activeOrders);
        
        // When
        ResponseEntity<List<Order>> response = tradingController.getActiveOrders();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("order1", response.getBody().get(0).getOrderId());
        assertEquals("order2", response.getBody().get(1).getOrderId());
    }
    
    @Test
    void testGetRecentOrders() {
        // Given
        List<Order> allOrders = List.of(
            new Order("order1", "BTC-USDT", Order.OrderSide.BUY, 
                     BigDecimal.valueOf(50000), BigDecimal.valueOf(0.1), true),
            new Order("order2", "ETH-USDT", Order.OrderSide.SELL, 
                     BigDecimal.valueOf(3000), BigDecimal.valueOf(1.0), true)
        );
        when(orderRepository.findAll()).thenReturn(allOrders);
        
        // When
        ResponseEntity<List<Order>> response = tradingController.getRecentOrders(10);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
    
    @Test
    void testGetRiskStatus() {
        // Given
        RiskManagement.RiskStatus riskStatus = new RiskManagement.RiskStatus(
            true, BigDecimal.valueOf(-10), BigDecimal.valueOf(-1), true
        );
        when(riskManagement.getRiskStatus(any(BigDecimal.class))).thenReturn(riskStatus);
        
        // When
        ResponseEntity<Map<String, Object>> response = tradingController.getRiskStatus();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertTrue((Boolean) body.get("dailyLossWithinLimit"));
        assertTrue((Boolean) body.get("tradingFrequencyOk"));
        assertTrue((Boolean) body.get("overallRiskAcceptable"));
    }
    
    @Test
    void testGetTradingStats() {
        // Given
        when(orderRepository.count()).thenReturn(100L);
        when(orderRepository.countByStatusAndIsVirtual(Order.OrderStatus.FILLED, true)).thenReturn(80L);
        when(orderRepository.countByStatusAndIsVirtual(Order.OrderStatus.CANCELLED, true)).thenReturn(5L);
        
        // When
        ResponseEntity<Map<String, Object>> response = tradingController.getTradingStats();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals(100L, body.get("totalOrders"));
        assertEquals(80L, body.get("filledOrders"));
        assertEquals(5L, body.get("cancelledOrders"));
        assertEquals(80.0, body.get("successRate"));
    }
}