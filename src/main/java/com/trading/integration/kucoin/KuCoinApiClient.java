package com.trading.integration.kucoin;

import com.trading.config.TradingBotConfig;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.kucoin.KucoinExchange;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * KuCoin API client for cryptocurrency trading operations
 */
@Service
public class KuCoinApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(KuCoinApiClient.class);
    
    @Autowired
    private TradingBotConfig config;
    
    private Exchange exchange;
    private MarketDataService marketDataService;
    private AccountService accountService;
    private TradeService tradeService;
    
    @PostConstruct
    public void initialize() {
        try {
            setupExchange();
            logger.info("KuCoin API client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize KuCoin API client", e);
        }
    }
    
    private void setupExchange() {
        ExchangeSpecification spec = new KucoinExchange().getDefaultExchangeSpecification();
        
        if (config.getKucoin().getApiKey() != null && !config.getKucoin().getApiKey().isEmpty()) {
            spec.setApiKey(config.getKucoin().getApiKey());
            spec.setSecretKey(config.getKucoin().getSecretKey());
            spec.setExchangeSpecificParametersItem("passphrase", config.getKucoin().getPassphrase());
        }
        
        if (config.getKucoin().isSandbox()) {
            spec.setExchangeSpecificParametersItem("Use_Sandbox", true);
            logger.info("Using KuCoin sandbox environment");
        }
        
        exchange = ExchangeFactory.INSTANCE.createExchange(spec);
        marketDataService = exchange.getMarketDataService();
        accountService = exchange.getAccountService();
        tradeService = exchange.getTradeService();
    }
    
    /**
     * Get current ticker for a trading pair
     */
    public Ticker getTicker(String tradingPair) throws IOException {
        CurrencyPair pair = parseCurrencyPair(tradingPair);
        return marketDataService.getTicker(pair);
    }
    
    /**
     * Get order book for a trading pair
     */
    public OrderBook getOrderBook(String tradingPair) throws IOException {
        CurrencyPair pair = parseCurrencyPair(tradingPair);
        return marketDataService.getOrderBook(pair);
    }
    
    /**
     * Get account information
     */
    public AccountInfo getAccountInfo() throws IOException {
        return accountService.getAccountInfo();
    }
    
    /**
     * Place a limit buy order
     */
    public String placeLimitBuyOrder(String tradingPair, BigDecimal amount, BigDecimal price) throws IOException {
        CurrencyPair pair = parseCurrencyPair(tradingPair);
        LimitOrder order = new LimitOrder(Order.OrderType.BID, amount, pair, null, null, price);
        return tradeService.placeLimitOrder(order);
    }
    
    /**
     * Place a limit sell order
     */
    public String placeLimitSellOrder(String tradingPair, BigDecimal amount, BigDecimal price) throws IOException {
        CurrencyPair pair = parseCurrencyPair(tradingPair);
        LimitOrder order = new LimitOrder(Order.OrderType.ASK, amount, pair, null, null, price);
        return tradeService.placeLimitOrder(order);
    }
    
    /**
     * Place a market buy order
     */
    public String placeMarketBuyOrder(String tradingPair, BigDecimal amount) throws IOException {
        CurrencyPair pair = parseCurrencyPair(tradingPair);
        MarketOrder order = new MarketOrder(Order.OrderType.BID, amount, pair);
        return tradeService.placeMarketOrder(order);
    }
    
    /**
     * Place a market sell order
     */
    public String placeMarketSellOrder(String tradingPair, BigDecimal amount) throws IOException {
        CurrencyPair pair = parseCurrencyPair(tradingPair);
        MarketOrder order = new MarketOrder(Order.OrderType.ASK, amount, pair);
        return tradeService.placeMarketOrder(order);
    }
    
    /**
     * Cancel an order
     */
    public boolean cancelOrder(String orderId) throws IOException {
        return tradeService.cancelOrder(orderId);
    }
    
    /**
     * Get open orders
     */
    public List<LimitOrder> getOpenOrders() throws IOException {
        return tradeService.getOpenOrders().getOpenOrders();
    }
    
    /**
     * Get order status
     */
    public org.knowm.xchange.dto.trade.UserTrade getOrderStatus(String orderId) throws IOException {
        // Note: This would need to be implemented based on KuCoin's specific API
        // For now, we'll return null and handle this in the service layer
        return null;
    }
    
    /**
     * Parse trading pair string to CurrencyPair object
     */
    private CurrencyPair parseCurrencyPair(String tradingPair) {
        String[] parts = tradingPair.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid trading pair format: " + tradingPair);
        }
        return new CurrencyPair(parts[0], parts[1]);
    }
    
    /**
     * Check if API is properly configured
     */
    public boolean isConfigured() {
        return config.getKucoin().getApiKey() != null && 
               !config.getKucoin().getApiKey().isEmpty() &&
               config.getKucoin().getSecretKey() != null &&
               !config.getKucoin().getSecretKey().isEmpty();
    }
    
    /**
     * Test API connectivity
     */
    public boolean testConnection() {
        try {
            if (!isConfigured()) {
                logger.warn("KuCoin API not configured, skipping connection test");
                return false;
            }
            
            // Try to get account info as a connectivity test
            getAccountInfo();
            logger.info("KuCoin API connection test successful");
            return true;
        } catch (Exception e) {
            logger.error("KuCoin API connection test failed", e);
            return false;
        }
    }
}