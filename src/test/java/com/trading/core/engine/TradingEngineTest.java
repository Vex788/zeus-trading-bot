package com.trading.core.engine;

import com.trading.config.TradingBotConfig;
import com.trading.core.ml.NeuralNetwork;
import com.trading.core.strategy.NeuralNetworkStrategy;
import com.trading.core.strategy.RiskManagement;
import com.trading.domain.entities.BotConfig;
import com.trading.domain.repositories.BotConfigRepository;
import com.trading.domain.repositories.PortfolioRepository;
import com.trading.domain.repositories.TradingHistoryRepository;
import com.trading.integration.kucoin.MarketDataFetcher;
import com.trading.integration.websocket.TradingUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TradingEngine
 */
@ExtendWith(MockitoExtension.class)
class TradingEngineTest {
    
    @Mock
    private TradingBotConfig config;
    
    @Mock
    private NeuralNetwork neuralNetwork;
    
    @Mock
    private NeuralNetworkStrategy strategy;
    
    @Mock
    private RiskManagement riskManagement;
    
    @Mock
    private OrderManager orderManager;
    
    @Mock
    private MarketDataFetcher marketDataFetcher;
    
    @Mock
    private TradingUpdateService updateService;
    
    @Mock
    private BotConfigRepository botConfigRepository;
    
    @Mock
    private PortfolioRepository portfolioRepository;
    
    @Mock
    private TradingHistoryRepository tradingHistoryRepository;
    
    @InjectMocks
    private TradingEngine tradingEngine;
    
    private BotConfig testBotConfig;
    
    @BeforeEach
    void setUp() {
        testBotConfig = new BotConfig(BotConfig.TradingMode.SHADOW, BigDecimal.valueOf(100));
        
        when(config.getVirtualBalance()).thenReturn(BigDecimal.valueOf(100));
        when(config.getTradingPairs()).thenReturn(java.util.List.of("BTC-USDT", "ETH-USDT"));
        when(botConfigRepository.findCurrentConfig()).thenReturn(Optional.of(testBotConfig));
        when(neuralNetwork.isInitialized()).thenReturn(true);
    }
    
    @Test
    void testStartTradingEngine() {
        // Given
        assertFalse(tradingEngine.isRunning());
        
        // When
        tradingEngine.start();
        
        // Then
        assertTrue(tradingEngine.isRunning());
        assertFalse(tradingEngine.isPaused());
        verify(updateService).sendBotStatusUpdate(eq("RUNNING"), eq("SHADOW"), any());
    }
    
    @Test
    void testStopTradingEngine() {
        // Given
        tradingEngine.start();
        assertTrue(tradingEngine.isRunning());
        
        // When
        tradingEngine.stop();
        
        // Then
        assertFalse(tradingEngine.isRunning());
        verify(updateService).sendBotStatusUpdate(eq("STOPPED"), eq("SHADOW"), any());
    }
    
    @Test
    void testPauseTradingEngine() {
        // Given
        tradingEngine.start();
        assertTrue(tradingEngine.isRunning());
        assertFalse(tradingEngine.isPaused());
        
        // When
        tradingEngine.pause();
        
        // Then
        assertTrue(tradingEngine.isRunning());
        assertTrue(tradingEngine.isPaused());
        verify(updateService).sendBotStatusUpdate(eq("PAUSED"), eq("SHADOW"), any());
    }
    
    @Test
    void testResumeTradingEngine() {
        // Given
        tradingEngine.start();
        tradingEngine.pause();
        assertTrue(tradingEngine.isPaused());
        
        // When
        tradingEngine.resume();
        
        // Then
        assertTrue(tradingEngine.isRunning());
        assertFalse(tradingEngine.isPaused());
        verify(updateService, times(2)).sendBotStatusUpdate(eq("RUNNING"), eq("SHADOW"), any());
    }
    
    @Test
    void testSwitchModeFromShadowToProduction() {
        // Given
        assertEquals(BotConfig.TradingMode.SHADOW, tradingEngine.getCurrentMode());
        
        // When
        tradingEngine.switchMode(BotConfig.TradingMode.PRODUCTION);
        
        // Then
        assertEquals(BotConfig.TradingMode.PRODUCTION, testBotConfig.getMode());
        verify(botConfigRepository).save(testBotConfig);
        verify(updateService).sendBotStatusUpdate(eq("MODE_CHANGED"), eq("PRODUCTION"), any());
    }
    
    @Test
    void testSwitchModeFromProductionToShadow() {
        // Given
        testBotConfig.setMode(BotConfig.TradingMode.PRODUCTION);
        
        // When
        tradingEngine.switchMode(BotConfig.TradingMode.SHADOW);
        
        // Then
        assertEquals(BotConfig.TradingMode.SHADOW, testBotConfig.getMode());
        verify(botConfigRepository).save(testBotConfig);
        verify(updateService).sendBotStatusUpdate(eq("MODE_CHANGED"), eq("SHADOW"), any());
    }
    
    @Test
    void testGetCurrentMode() {
        // When
        BotConfig.TradingMode mode = tradingEngine.getCurrentMode();
        
        // Then
        assertEquals(BotConfig.TradingMode.SHADOW, mode);
    }
    
    @Test
    void testInitializationWithMissingConfig() {
        // Given
        when(botConfigRepository.findCurrentConfig()).thenReturn(Optional.empty());
        when(botConfigRepository.save(any(BotConfig.class))).thenReturn(testBotConfig);
        
        // When
        tradingEngine.initialize();
        
        // Then
        verify(botConfigRepository).save(any(BotConfig.class));
    }
}