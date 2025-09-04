package com.trading.integration.kucoin;

import com.trading.config.TradingBotConfig;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Service for fetching and caching market data from KuCoin
 */
@Service
public class MarketDataFetcher {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataFetcher.class);
    
    @Autowired
    private KuCoinApiClient apiClient;
    
    @Autowired
    private TradingBotConfig config;
    
    // Cache for market data
    private final Map<String, MarketData> marketDataCache = new ConcurrentHashMap<>();
    private final Map<String, OrderBook> orderBookCache = new ConcurrentHashMap<>();
    
    /**
     * Fetch market data for all configured trading pairs
     */
    @Scheduled(fixedDelayString = "${app.scheduler.data-fetch-interval:10000}")
    public void fetchMarketData() {
        for (String tradingPair : config.getTradingPairs()) {
            fetchMarketDataAsync(tradingPair);
        }
    }
    
    /**
     * Asynchronously fetch market data for a specific trading pair
     */
    @Async
    public CompletableFuture<MarketData> fetchMarketDataAsync(String tradingPair) {
        try {
            Ticker ticker = apiClient.getTicker(tradingPair);
            OrderBook orderBook = apiClient.getOrderBook(tradingPair);
            
            MarketData marketData = new MarketData(
                tradingPair,
                ticker.getLast(),
                ticker.getBid(),
                ticker.getAsk(),
                ticker.getVolume(),
                LocalDateTime.now()
            );
            
            marketDataCache.put(tradingPair, marketData);
            orderBookCache.put(tradingPair, orderBook);
            
            logger.debug("Updated market data for {}: price={}, volume={}", 
                        tradingPair, ticker.getLast(), ticker.getVolume());
            
            return CompletableFuture.completedFuture(marketData);
            
        } catch (IOException e) {
            logger.error("Failed to fetch market data for {}", tradingPair, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Get cached market data for a trading pair
     */
    public MarketData getMarketData(String tradingPair) {
        return marketDataCache.get(tradingPair);
    }
    
    /**
     * Get cached order book for a trading pair
     */
    public OrderBook getOrderBook(String tradingPair) {
        return orderBookCache.get(tradingPair);
    }
    
    /**
     * Get current price for a trading pair
     */
    public BigDecimal getCurrentPrice(String tradingPair) {
        MarketData data = marketDataCache.get(tradingPair);
        if (data != null && data.isRecent()) {
            return data.getLastPrice();
        }
        
        // If no cached data or data is stale, fetch fresh data
        try {
            Ticker ticker = apiClient.getTicker(tradingPair);
            return ticker.getLast();
        } catch (IOException e) {
            logger.error("Failed to get current price for {}", tradingPair, e);
            return null;
        }
    }
    
    /**
     * Get bid price for a trading pair
     */
    public BigDecimal getBidPrice(String tradingPair) {
        MarketData data = marketDataCache.get(tradingPair);
        if (data != null && data.isRecent()) {
            return data.getBidPrice();
        }
        return getCurrentPrice(tradingPair); // Fallback to last price
    }
    
    /**
     * Get ask price for a trading pair
     */
    public BigDecimal getAskPrice(String tradingPair) {
        MarketData data = marketDataCache.get(tradingPair);
        if (data != null && data.isRecent()) {
            return data.getAskPrice();
        }
        return getCurrentPrice(tradingPair); // Fallback to last price
    }
    
    /**
     * Get 24h volume for a trading pair
     */
    public BigDecimal getVolume(String tradingPair) {
        MarketData data = marketDataCache.get(tradingPair);
        return data != null ? data.getVolume() : BigDecimal.ZERO;
    }
    
    /**
     * Check if market data is available for all configured pairs
     */
    public boolean isMarketDataAvailable() {
        for (String tradingPair : config.getTradingPairs()) {
            if (!marketDataCache.containsKey(tradingPair)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get all cached market data
     */
    public Map<String, MarketData> getAllMarketData() {
        return Map.copyOf(marketDataCache);
    }
    
    /**
     * Market data container class
     */
    public static class MarketData {
        private final String tradingPair;
        private final BigDecimal lastPrice;
        private final BigDecimal bidPrice;
        private final BigDecimal askPrice;
        private final BigDecimal volume;
        private final LocalDateTime timestamp;
        
        public MarketData(String tradingPair, BigDecimal lastPrice, BigDecimal bidPrice, 
                         BigDecimal askPrice, BigDecimal volume, LocalDateTime timestamp) {
            this.tradingPair = tradingPair;
            this.lastPrice = lastPrice;
            this.bidPrice = bidPrice;
            this.askPrice = askPrice;
            this.volume = volume;
            this.timestamp = timestamp;
        }
        
        public boolean isRecent() {
            return timestamp.isAfter(LocalDateTime.now().minusMinutes(1));
        }
        
        // Getters
        public String getTradingPair() { return tradingPair; }
        public BigDecimal getLastPrice() { return lastPrice; }
        public BigDecimal getBidPrice() { return bidPrice; }
        public BigDecimal getAskPrice() { return askPrice; }
        public BigDecimal getVolume() { return volume; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}