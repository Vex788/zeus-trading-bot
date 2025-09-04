package com.trading.api.controllers;

import com.trading.core.engine.TradingEngine;
import com.trading.core.ml.NeuralNetwork;
import com.trading.domain.entities.TradingHistory;
import com.trading.domain.repositories.MLPredictionRepository;
import com.trading.domain.repositories.PortfolioRepository;
import com.trading.domain.repositories.TradingHistoryRepository;
import com.trading.integration.kucoin.MarketDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for dashboard data
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    @Autowired
    private TradingEngine tradingEngine;
    
    @Autowired
    private NeuralNetwork neuralNetwork;
    
    @Autowired
    private MarketDataFetcher marketDataFetcher;
    
    @Autowired
    private TradingHistoryRepository tradingHistoryRepository;
    
    @Autowired
    private MLPredictionRepository mlPredictionRepository;
    
    @Autowired
    private PortfolioRepository portfolioRepository;
    
    /**
     * Get main dashboard data
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        try {
            // Get current portfolio value
            BigDecimal totalValue = portfolioRepository.calculateTotalValue(true);
            if (totalValue == null) totalValue = BigDecimal.valueOf(100);
            
            // Get profit/loss
            BigDecimal initialValue = BigDecimal.valueOf(100); // Initial virtual balance
            BigDecimal profitLoss = totalValue.subtract(initialValue);
            BigDecimal profitLossPercent = profitLoss.divide(initialValue, 4, RoundingMode.HALF_UP)
                                                   .multiply(BigDecimal.valueOf(100));
            
            // Get recent trades count
            LocalDateTime last24h = LocalDateTime.now().minusHours(24);
            List<TradingHistory> recentTrades = tradingHistoryRepository.findRecentTrades(last24h);
            
            // Get ML accuracy
            BigDecimal mlAccuracy = mlPredictionRepository.calculateOverallAccuracy();
            if (mlAccuracy == null) mlAccuracy = BigDecimal.valueOf(0.75);
            
            return ResponseEntity.ok(Map.of(
                "totalValue", totalValue.toString(),
                "profitLoss", profitLoss.toString(),
                "profitLossPercent", profitLossPercent.toString(),
                "recentTradesCount", recentTrades.size(),
                "mlAccuracy", mlAccuracy.multiply(BigDecimal.valueOf(100)).toString(),
                "botStatus", getBotStatus(),
                "lastUpdate", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get dashboard overview", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load dashboard data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get portfolio data
     */
    @GetMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> getPortfolioData() {
        try {
            var virtualPortfolios = portfolioRepository.findByIsVirtualTrue();
            
            List<Map<String, Object>> portfolioData = virtualPortfolios.stream()
                .map(portfolio -> Map.of(
                    "currency", portfolio.getCurrency(),
                    "balance", portfolio.getBalance().toString(),
                    "availableBalance", portfolio.getAvailableBalance().toString(),
                    "lockedBalance", portfolio.getLockedBalance().toString(),
                    "lastUpdated", portfolio.getLastUpdated().toString()
                ))
                .collect(Collectors.toList());
            
            BigDecimal totalValue = portfolioRepository.calculateTotalValue(true);
            if (totalValue == null) totalValue = BigDecimal.valueOf(100);
            
            return ResponseEntity.ok(Map.of(
                "portfolios", portfolioData,
                "totalValue", totalValue.toString(),
                "currency", "USDT"
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get portfolio data", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load portfolio data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get market data for all trading pairs
     */
    @GetMapping("/market-data")
    public ResponseEntity<Map<String, Object>> getMarketData() {
        try {
            Map<String, MarketDataFetcher.MarketData> allMarketData = marketDataFetcher.getAllMarketData();
            
            Map<String, Map<String, Object>> marketDataResponse = allMarketData.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        MarketDataFetcher.MarketData data = entry.getValue();
                        return Map.of(
                            "lastPrice", data.getLastPrice().toString(),
                            "bidPrice", data.getBidPrice().toString(),
                            "askPrice", data.getAskPrice().toString(),
                            "volume", data.getVolume().toString(),
                            "timestamp", data.getTimestamp().toString()
                        );
                    }
                ));
            
            return ResponseEntity.ok(Map.of(
                "marketData", marketDataResponse,
                "lastUpdate", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get market data", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load market data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get trading history for charts
     */
    @GetMapping("/trading-history")
    public ResponseEntity<Map<String, Object>> getTradingHistory(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
            List<TradingHistory> trades = tradingHistoryRepository.findTradesBetweenDates(
                cutoffTime, LocalDateTime.now());
            
            List<Map<String, Object>> tradesData = trades.stream()
                .map(trade -> Map.of(
                    "timestamp", trade.getTradeTime().toString(),
                    "tradingPair", trade.getTradingPair(),
                    "action", trade.getAction().toString(),
                    "amount", trade.getAmount().toString(),
                    "price", trade.getPrice().toString(),
                    "profitLoss", trade.getProfitLoss() != null ? trade.getProfitLoss().toString() : "0",
                    "balanceAfter", trade.getBalanceAfter().toString()
                ))
                .collect(Collectors.toList());
            
            // Calculate cumulative profit/loss for chart
            BigDecimal cumulativePL = BigDecimal.ZERO;
            for (Map<String, Object> trade : tradesData) {
                BigDecimal pl = new BigDecimal(trade.get("profitLoss").toString());
                cumulativePL = cumulativePL.add(pl);
                trade.put("cumulativeProfitLoss", cumulativePL.toString());
            }
            
            return ResponseEntity.ok(Map.of(
                "trades", tradesData,
                "totalTrades", trades.size(),
                "timeRange", hours + " hours"
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get trading history", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load trading history: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get ML performance metrics
     */
    @GetMapping("/ml/performance")
    public ResponseEntity<Map<String, Object>> getMLPerformance() {
        try {
            BigDecimal overallAccuracy = mlPredictionRepository.calculateOverallAccuracy();
            if (overallAccuracy == null) overallAccuracy = BigDecimal.valueOf(0.75);
            
            long totalPredictions = mlPredictionRepository.count();
            long correctPredictions = mlPredictionRepository.countCorrectPredictions("", BigDecimal.valueOf(0.7));
            
            NeuralNetwork.NetworkMetrics metrics = neuralNetwork.getMetrics();
            
            return ResponseEntity.ok(Map.of(
                "overallAccuracy", overallAccuracy.multiply(BigDecimal.valueOf(100)).toString(),
                "totalPredictions", totalPredictions,
                "correctPredictions", correctPredictions,
                "networkParameters", metrics.getParameters(),
                "networkLoss", metrics.getLoss(),
                "isInitialized", neuralNetwork.isInitialized()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get ML performance", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load ML performance: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get recent ML predictions
     */
    @GetMapping("/ml/predictions")
    public ResponseEntity<List<Map<String, Object>>> getRecentPredictions(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
            var predictions = mlPredictionRepository.findPredictionsBetweenDates(
                cutoffTime, LocalDateTime.now())
                .stream()
                .limit(limit)
                .map(prediction -> Map.of(
                    "timestamp", prediction.getPredictionTime().toString(),
                    "tradingPair", prediction.getTradingPair(),
                    "predictedPrice", prediction.getPredictedPrice() != null ? 
                                   prediction.getPredictedPrice().toString() : "N/A",
                    "actualPrice", prediction.getActualPrice() != null ? 
                                 prediction.getActualPrice().toString() : "N/A",
                    "accuracy", prediction.getAccuracyScore() != null ? 
                              prediction.getAccuracyScore().toString() : "N/A",
                    "direction", prediction.getDirection() != null ? 
                               prediction.getDirection().toString() : "N/A",
                    "confidence", prediction.getConfidenceLevel() != null ? 
                                prediction.getConfidenceLevel().toString() : "N/A"
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(predictions);
            
        } catch (Exception e) {
            logger.error("Failed to get recent predictions", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get profit/loss chart data
     */
    @GetMapping("/chart/profit-loss")
    public ResponseEntity<Map<String, Object>> getProfitLossChart(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
            List<TradingHistory> trades = tradingHistoryRepository.findTradesBetweenDates(
                cutoffTime, LocalDateTime.now());
            
            // Group trades by hour and calculate cumulative P&L
            Map<String, BigDecimal> hourlyPL = trades.stream()
                .collect(Collectors.groupingBy(
                    trade -> trade.getTradeTime().withMinute(0).withSecond(0).toString(),
                    Collectors.reducing(BigDecimal.ZERO,
                        trade -> trade.getProfitLoss() != null ? trade.getProfitLoss() : BigDecimal.ZERO,
                        BigDecimal::add)
                ));
            
            return ResponseEntity.ok(Map.of(
                "chartData", hourlyPL,
                "timeRange", hours + " hours"
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get profit/loss chart data", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load chart data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get bot status information
     */
    private Map<String, Object> getBotStatus() {
        return Map.of(
            "isRunning", tradingEngine.isRunning(),
            "isPaused", tradingEngine.isPaused(),
            "mode", tradingEngine.getCurrentMode().toString(),
            "status", getStatusString()
        );
    }
    
    /**
     * Get status string
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