package com.trading.api.controllers;

import com.trading.api.dto.TradingConfigDto;
import com.trading.api.dto.TradingResponse;
import com.trading.core.engine.TradingEngine;
import com.trading.core.strategy.RiskManagement;
import com.trading.domain.entities.BotConfig;
import com.trading.domain.entities.Order;
import com.trading.domain.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for trading operations
 */
@RestController
@RequestMapping("/api/trading")
@CrossOrigin(origins = "*")
public class TradingController {
    
    private static final Logger logger = LoggerFactory.getLogger(TradingController.class);
    
    @Autowired
    private TradingEngine tradingEngine;
    
    @Autowired
    private RiskManagement riskManagement;
    
    @Autowired
    private OrderRepository orderRepository;
    
    /**
     * Start the trading bot
     */
    @PostMapping("/start")
    public ResponseEntity<TradingResponse> startTrading(@RequestBody(required = false) TradingConfigDto config) {
        try {
            if (!tradingEngine.isRunning()) {
                tradingEngine.start();
                logger.info("Trading bot started via API");
                
                return ResponseEntity.ok(new TradingResponse(
                    true,
                    "Trading bot started successfully",
                    Map.of(
                        "status", "RUNNING",
                        "mode", tradingEngine.getCurrentMode().toString()
                    )
                ));
            } else {
                return ResponseEntity.ok(new TradingResponse(
                    false,
                    "Trading bot is already running",
                    Map.of("status", "ALREADY_RUNNING")
                ));
            }
            
        } catch (Exception e) {
            logger.error("Failed to start trading bot", e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "Failed to start trading bot: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Stop the trading bot
     */
    @PostMapping("/stop")
    public ResponseEntity<TradingResponse> stopTrading() {
        try {
            if (tradingEngine.isRunning()) {
                tradingEngine.stop();
                logger.info("Trading bot stopped via API");
                
                return ResponseEntity.ok(new TradingResponse(
                    true,
                    "Trading bot stopped successfully",
                    Map.of("status", "STOPPED")
                ));
            } else {
                return ResponseEntity.ok(new TradingResponse(
                    false,
                    "Trading bot is not running",
                    Map.of("status", "NOT_RUNNING")
                ));
            }
            
        } catch (Exception e) {
            logger.error("Failed to stop trading bot", e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "Failed to stop trading bot: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Pause the trading bot
     */
    @PostMapping("/pause")
    public ResponseEntity<TradingResponse> pauseTrading() {
        try {
            if (tradingEngine.isRunning() && !tradingEngine.isPaused()) {
                tradingEngine.pause();
                logger.info("Trading bot paused via API");
                
                return ResponseEntity.ok(new TradingResponse(
                    true,
                    "Trading bot paused successfully",
                    Map.of("status", "PAUSED")
                ));
            } else {
                return ResponseEntity.ok(new TradingResponse(
                    false,
                    "Trading bot cannot be paused",
                    Map.of(
                        "status", tradingEngine.isRunning() ? "ALREADY_PAUSED" : "NOT_RUNNING"
                    )
                ));
            }
            
        } catch (Exception e) {
            logger.error("Failed to pause trading bot", e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "Failed to pause trading bot: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Resume the trading bot
     */
    @PostMapping("/resume")
    public ResponseEntity<TradingResponse> resumeTrading() {
        try {
            if (tradingEngine.isRunning() && tradingEngine.isPaused()) {
                tradingEngine.resume();
                logger.info("Trading bot resumed via API");
                
                return ResponseEntity.ok(new TradingResponse(
                    true,
                    "Trading bot resumed successfully",
                    Map.of("status", "RUNNING")
                ));
            } else {
                return ResponseEntity.ok(new TradingResponse(
                    false,
                    "Trading bot cannot be resumed",
                    Map.of(
                        "status", tradingEngine.isRunning() ? "NOT_PAUSED" : "NOT_RUNNING"
                    )
                ));
            }
            
        } catch (Exception e) {
            logger.error("Failed to resume trading bot", e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "Failed to resume trading bot: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Switch trading mode between PRODUCTION and SHADOW
     */
    @PostMapping("/mode/switch")
    public ResponseEntity<TradingResponse> switchMode(@RequestParam String mode) {
        try {
            BotConfig.TradingMode newMode = BotConfig.TradingMode.valueOf(mode.toUpperCase());
            BotConfig.TradingMode currentMode = tradingEngine.getCurrentMode();
            
            if (currentMode == newMode) {
                return ResponseEntity.ok(new TradingResponse(
                    false,
                    "Already in " + mode + " mode",
                    Map.of("currentMode", currentMode.toString())
                ));
            }
            
            tradingEngine.switchMode(newMode);
            logger.info("Trading mode switched from {} to {}", currentMode, newMode);
            
            return ResponseEntity.ok(new TradingResponse(
                true,
                "Trading mode switched successfully",
                Map.of(
                    "oldMode", currentMode.toString(),
                    "newMode", newMode.toString()
                )
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new TradingResponse(
                false,
                "Invalid mode: " + mode + ". Valid modes: PRODUCTION, SHADOW",
                null
            ));
        } catch (Exception e) {
            logger.error("Failed to switch trading mode", e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "Failed to switch mode: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Get current trading status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTradingStatus() {
        try {
            return ResponseEntity.ok(Map.of(
                "isRunning", tradingEngine.isRunning(),
                "isPaused", tradingEngine.isPaused(),
                "mode", tradingEngine.getCurrentMode().toString(),
                "status", getStatusString()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get trading status", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get status: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get active orders
     */
    @GetMapping("/orders/active")
    public ResponseEntity<List<Order>> getActiveOrders() {
        try {
            List<Order> activeOrders = orderRepository.findActiveOrders();
            return ResponseEntity.ok(activeOrders);
            
        } catch (Exception e) {
            logger.error("Failed to get active orders", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get recent orders
     */
    @GetMapping("/orders/recent")
    public ResponseEntity<List<Order>> getRecentOrders(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Order> recentOrders = orderRepository.findAll()
                .stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(limit)
                .toList();
            
            return ResponseEntity.ok(recentOrders);
            
        } catch (Exception e) {
            logger.error("Failed to get recent orders", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Cancel an order
     */
    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<TradingResponse> cancelOrder(@PathVariable String orderId) {
        try {
            // Implementation would depend on OrderManager
            // For now, return a placeholder response
            
            return ResponseEntity.ok(new TradingResponse(
                true,
                "Order cancellation requested",
                Map.of("orderId", orderId)
            ));
            
        } catch (Exception e) {
            logger.error("Failed to cancel order: {}", orderId, e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "Failed to cancel order: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Get risk management status
     */
    @GetMapping("/risk/status")
    public ResponseEntity<Map<String, Object>> getRiskStatus() {
        try {
            // This would need current balance - simplified for now
            BigDecimal currentBalance = BigDecimal.valueOf(100); // Placeholder
            RiskManagement.RiskStatus riskStatus = riskManagement.getRiskStatus(currentBalance);
            
            return ResponseEntity.ok(Map.of(
                "dailyLossWithinLimit", riskStatus.isDailyLossWithinLimit(),
                "dailyProfitLoss", riskStatus.getDailyProfitLoss().toString(),
                "dailyProfitLossPercent", riskStatus.getDailyProfitLossPercent().toString(),
                "tradingFrequencyOk", riskStatus.isTradingFrequencyOk(),
                "overallRiskAcceptable", riskStatus.isOverallRiskAcceptable()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get risk status", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get risk status: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get trading statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTradingStats() {
        try {
            long totalOrders = orderRepository.count();
            long filledOrders = orderRepository.countByStatusAndIsVirtual(Order.OrderStatus.FILLED, true);
            long cancelledOrders = orderRepository.countByStatusAndIsVirtual(Order.OrderStatus.CANCELLED, true);
            
            return ResponseEntity.ok(Map.of(
                "totalOrders", totalOrders,
                "filledOrders", filledOrders,
                "cancelledOrders", cancelledOrders,
                "successRate", totalOrders > 0 ? (double) filledOrders / totalOrders * 100 : 0.0
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get trading stats", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get stats: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get status string based on current state
     */
    private String getStatusString() {
        if (!tradingEngine.isRunning()) {
            return "STOPPED";
        } else if (tradingEngine.isPaused()) {
            return "PAUSED";
        } else {
            return "RUNNING";
        }
    }
}