package com.zeus.trading.bot.web;

import com.zeus.trading.bot.AdvancedTradingStrategy;
import com.zeus.trading.bot.web.model.TradingData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the trading web interface.
 */
@Controller
public class TradingController {

    @Autowired
    AdvancedTradingStrategy tradingStrategy;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final Map<String, TradingData> tradingDataMap = new HashMap<>();

    /**
     * Main page of the trading interface.
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currencyPairs", tradingStrategy.getRequestedCurrencyPairs());
        return "index";
    }

    /**
     * Get trading data for a specific currency pair.
     */
    @GetMapping("/api/trading-data")
    @ResponseBody
    public TradingData getTradingData(@RequestParam String currencyPair) {
        return tradingDataMap.getOrDefault(currencyPair, new TradingData());
    }

    /**
     * Create a new order.
     */
    @PostMapping("/api/create-order")
    @ResponseBody
    public Map<String, Object> createOrder(@RequestParam String currencyPair, 
                                          @RequestParam String type,
                                          @RequestParam BigDecimal amount) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Here we would call the trading strategy to create an order
            // For now, we just return a success message
            response.put("success", true);
            response.put("message", "Order created successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create order: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Close a position.
     */
    @PostMapping("/api/close-position")
    @ResponseBody
    public Map<String, Object> closePosition(@RequestParam String positionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Here we would call the trading strategy to close the position
            // For now, we just return a success message
            response.put("success", true);
            response.put("message", "Position closed successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to close position: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * WebSocket endpoint to receive trading data updates.
     */
    @MessageMapping("/trading-data")
    @SendTo("/topic/trading-data")
    public TradingData sendTradingData(TradingData tradingData) {
        return tradingData;
    }

    /**
     * Update trading data and send it to clients via WebSocket.
     * This method would be called by the trading strategy when new data is available.
     */
    public void updateTradingData(TradingData tradingData) {
        tradingDataMap.put(tradingData.getCurrencyPair(), tradingData);
        messagingTemplate.convertAndSend("/topic/trading-data/" + tradingData.getCurrencyPair(), tradingData);
    }
}