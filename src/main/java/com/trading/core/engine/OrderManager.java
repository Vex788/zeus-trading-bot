package com.trading.core.engine;

import com.trading.domain.entities.Order;
import com.trading.domain.entities.Portfolio;
import com.trading.domain.repositories.OrderRepository;
import com.trading.domain.repositories.PortfolioRepository;
import com.trading.integration.kucoin.KuCoinApiClient;
import com.trading.integration.websocket.TradingUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Order management service for handling both real and virtual orders
 */
@Service
public class OrderManager {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderManager.class);
    
    @Autowired
    private KuCoinApiClient apiClient;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PortfolioRepository portfolioRepository;
    
    @Autowired
    private TradingUpdateService updateService;
    
    /**
     * Execute a real order on KuCoin exchange
     */
    @Transactional
    public OrderResult executeRealOrder(String tradingPair, TradingDecision.Action action, 
                                       BigDecimal amount, BigDecimal price) {
        try {
            if (!apiClient.isConfigured()) {
                throw new IllegalStateException("KuCoin API not configured for real trading");
            }
            
            String orderId;
            
            // Place order on exchange
            if (action == TradingDecision.Action.BUY) {
                orderId = apiClient.placeMarketBuyOrder(tradingPair, amount);
            } else {
                orderId = apiClient.placeMarketSellOrder(tradingPair, amount);
            }
            
            // Create order record
            Order order = new Order(
                orderId,
                tradingPair,
                action == TradingDecision.Action.BUY ? Order.OrderSide.BUY : Order.OrderSide.SELL,
                price,
                amount,
                false // Not virtual
            );
            
            order.setStatus(Order.OrderStatus.PENDING);
            orderRepository.save(order);
            
            // Send order update
            updateService.sendOrderUpdate(Map.of(
                "orderId", orderId,
                "tradingPair", tradingPair,
                "action", action.toString(),
                "amount", amount.toString(),
                "price", price.toString(),
                "status", "PENDING",
                "isVirtual", false
            ));
            
            logger.info("Real order placed: {} {} {} at {}", action, amount, tradingPair, price);
            
            return new OrderResult(true, orderId, "Order placed successfully", BigDecimal.valueOf(0.8));
            
        } catch (Exception e) {
            logger.error("Failed to execute real order", e);
            return new OrderResult(false, null, "Failed to place order: " + e.getMessage(), BigDecimal.ZERO);
        }
    }
    
    /**
     * Execute a virtual order for shadow mode
     */
    @Transactional
    public OrderResult executeVirtualOrder(String tradingPair, TradingDecision.Action action, 
                                         BigDecimal amount, BigDecimal price) {
        try {
            String orderId = "VIRTUAL_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Calculate trade value
            BigDecimal tradeValue = amount.multiply(price);
            
            // Get or create portfolio entries
            String baseCurrency = tradingPair.split("-")[0]; // e.g., BTC from BTC-USDT
            String quoteCurrency = tradingPair.split("-")[1]; // e.g., USDT from BTC-USDT
            
            Portfolio basePortfolio = portfolioRepository
                .findByCurrencyAndIsVirtual(baseCurrency, true)
                .orElse(new Portfolio(baseCurrency, BigDecimal.ZERO, true));
            
            Portfolio quotePortfolio = portfolioRepository
                .findByCurrencyAndIsVirtual(quoteCurrency, true)
                .orElse(new Portfolio(quoteCurrency, BigDecimal.valueOf(100), true));
            
            // Execute virtual trade
            boolean tradeExecuted = false;
            
            if (action == TradingDecision.Action.BUY) {
                // Buy: spend quote currency, get base currency
                if (quotePortfolio.getAvailableBalance().compareTo(tradeValue) >= 0) {
                    quotePortfolio.updateBalance(quotePortfolio.getBalance().subtract(tradeValue));
                    basePortfolio.updateBalance(basePortfolio.getBalance().add(amount));
                    tradeExecuted = true;
                }
            } else {
                // Sell: spend base currency, get quote currency
                if (basePortfolio.getAvailableBalance().compareTo(amount) >= 0) {
                    basePortfolio.updateBalance(basePortfolio.getBalance().subtract(amount));
                    quotePortfolio.updateBalance(quotePortfolio.getBalance().add(tradeValue));
                    tradeExecuted = true;
                }
            }
            
            if (!tradeExecuted) {
                return new OrderResult(false, null, "Insufficient virtual balance", BigDecimal.ZERO);
            }
            
            // Save portfolio updates
            portfolioRepository.save(basePortfolio);
            portfolioRepository.save(quotePortfolio);
            
            // Create order record
            Order order = new Order(
                orderId,
                tradingPair,
                action == TradingDecision.Action.BUY ? Order.OrderSide.BUY : Order.OrderSide.SELL,
                price,
                amount,
                true // Virtual
            );
            
            order.setStatus(Order.OrderStatus.FILLED);
            order.setFilledAt(LocalDateTime.now());
            order.setFilledAmount(amount);
            orderRepository.save(order);
            
            // Send order update
            updateService.sendOrderUpdate(Map.of(
                "orderId", orderId,
                "tradingPair", tradingPair,
                "action", action.toString(),
                "amount", amount.toString(),
                "price", price.toString(),
                "status", "FILLED",
                "isVirtual", true
            ));
            
            logger.info("Virtual order executed: {} {} {} at {} (Virtual)", 
                       action, amount, tradingPair, price);
            
            return new OrderResult(true, orderId, "Virtual order executed successfully", BigDecimal.valueOf(0.9));
            
        } catch (Exception e) {
            logger.error("Failed to execute virtual order", e);
            return new OrderResult(false, null, "Failed to execute virtual order: " + e.getMessage(), BigDecimal.ZERO);
        }
    }
    
    /**
     * Cancel an order
     */
    @Transactional
    public boolean cancelOrder(String orderId) {
        try {
            Order order = orderRepository.findByOrderId(orderId).orElse(null);
            if (order == null) {
                logger.warn("Order not found: {}", orderId);
                return false;
            }
            
            if (order.getIsVirtual()) {
                // Cancel virtual order
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
                
                logger.info("Virtual order cancelled: {}", orderId);
                return true;
            } else {
                // Cancel real order
                boolean cancelled = apiClient.cancelOrder(orderId);
                if (cancelled) {
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    
                    logger.info("Real order cancelled: {}", orderId);
                }
                return cancelled;
            }
            
        } catch (Exception e) {
            logger.error("Failed to cancel order: {}", orderId, e);
            return false;
        }
    }
    
    /**
     * Get order status
     */
    public Order.OrderStatus getOrderStatus(String orderId) {
        return orderRepository.findByOrderId(orderId)
            .map(Order::getStatus)
            .orElse(Order.OrderStatus.FAILED);
    }
    
    /**
     * Calculate trading fees (simplified)
     */
    private BigDecimal calculateFees(BigDecimal amount, BigDecimal price) {
        BigDecimal tradeValue = amount.multiply(price);
        return tradeValue.multiply(BigDecimal.valueOf(0.001)); // 0.1% fee
    }
    
    /**
     * Validate order parameters
     */
    private boolean validateOrder(String tradingPair, BigDecimal amount, BigDecimal price) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid amount: {}", amount);
            return false;
        }
        
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid price: {}", price);
            return false;
        }
        
        // Add more validation as needed (minimum order size, etc.)
        return true;
    }
}