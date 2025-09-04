package com.trading.integration.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for sending real-time trading updates via WebSocket
 */
@Service
public class TradingUpdateService {
    
    private static final Logger logger = LoggerFactory.getLogger(TradingUpdateService.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String TOPIC_TRADING_UPDATES = "/topic/trading-updates";
    private static final String TOPIC_MARKET_DATA = "/topic/market-data";
    private static final String TOPIC_PORTFOLIO = "/topic/portfolio";
    private static final String TOPIC_ORDERS = "/topic/orders";
    
    /**
     * Send trading update to all connected clients
     */
    public void sendTradingUpdate(TradingUpdate update) {
        try {
            messagingTemplate.convertAndSend(TOPIC_TRADING_UPDATES, update);
            logger.debug("Sent trading update: {}", update.getType());
        } catch (Exception e) {
            logger.error("Failed to send trading update", e);
        }
    }
    
    /**
     * Send market data update
     */
    public void sendMarketDataUpdate(Map<String, Object> marketData) {
        try {
            messagingTemplate.convertAndSend(TOPIC_MARKET_DATA, marketData);
            logger.debug("Sent market data update for {} pairs", marketData.size());
        } catch (Exception e) {
            logger.error("Failed to send market data update", e);
        }
    }
    
    /**
     * Send portfolio update
     */
    public void sendPortfolioUpdate(Map<String, Object> portfolio) {
        try {
            messagingTemplate.convertAndSend(TOPIC_PORTFOLIO, portfolio);
            logger.debug("Sent portfolio update");
        } catch (Exception e) {
            logger.error("Failed to send portfolio update", e);
        }
    }
    
    /**
     * Send order update
     */
    public void sendOrderUpdate(Map<String, Object> order) {
        try {
            messagingTemplate.convertAndSend(TOPIC_ORDERS, order);
            logger.debug("Sent order update");
        } catch (Exception e) {
            logger.error("Failed to send order update", e);
        }
    }
    
    /**
     * Send bot status update
     */
    public void sendBotStatusUpdate(String status, String mode, Map<String, Object> details) {
        TradingUpdate update = new TradingUpdate(
            TradingUpdateType.BOT_STATUS,
            Map.of(
                "status", status,
                "mode", mode,
                "details", details,
                "timestamp", LocalDateTime.now()
            )
        );
        sendTradingUpdate(update);
    }
    
    /**
     * Send trade execution update
     */
    public void sendTradeExecutionUpdate(String tradingPair, String action, 
                                       String amount, String price, boolean isVirtual) {
        TradingUpdate update = new TradingUpdate(
            TradingUpdateType.TRADE_EXECUTED,
            Map.of(
                "tradingPair", tradingPair,
                "action", action,
                "amount", amount,
                "price", price,
                "isVirtual", isVirtual,
                "timestamp", LocalDateTime.now()
            )
        );
        sendTradingUpdate(update);
    }
    
    /**
     * Send ML prediction update
     */
    public void sendMLPredictionUpdate(String tradingPair, String prediction, 
                                     String confidence, String direction) {
        TradingUpdate update = new TradingUpdate(
            TradingUpdateType.ML_PREDICTION,
            Map.of(
                "tradingPair", tradingPair,
                "prediction", prediction,
                "confidence", confidence,
                "direction", direction,
                "timestamp", LocalDateTime.now()
            )
        );
        sendTradingUpdate(update);
    }
    
    /**
     * Trading update container class
     */
    public static class TradingUpdate {
        private TradingUpdateType type;
        private Map<String, Object> data;
        private LocalDateTime timestamp;
        
        public TradingUpdate(TradingUpdateType type, Map<String, Object> data) {
            this.type = type;
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters and Setters
        public TradingUpdateType getType() { return type; }
        public void setType(TradingUpdateType type) { this.type = type; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Trading update types
     */
    public enum TradingUpdateType {
        BOT_STATUS,
        TRADE_EXECUTED,
        ORDER_PLACED,
        ORDER_FILLED,
        ORDER_CANCELLED,
        ML_PREDICTION,
        MARKET_DATA,
        PORTFOLIO_UPDATE,
        ERROR,
        WARNING
    }
}