package com.trading.core.engine;

import com.trading.config.TradingBotConfig;
import com.trading.core.ml.DataPreprocessor;
import com.trading.core.ml.NeuralNetwork;
import com.trading.core.ml.TradingResult;
import com.trading.core.strategy.NeuralNetworkStrategy;
import com.trading.core.strategy.RiskManagement;
import com.trading.domain.entities.BotConfig;
import com.trading.domain.entities.Order;
import com.trading.domain.entities.Portfolio;
import com.trading.domain.entities.TradingHistory;
import com.trading.domain.repositories.BotConfigRepository;
import com.trading.domain.repositories.PortfolioRepository;
import com.trading.domain.repositories.TradingHistoryRepository;
import com.trading.integration.kucoin.MarketDataFetcher;
import com.trading.integration.websocket.TradingUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main trading engine that orchestrates all trading operations
 * Supports both PRODUCTION and SHADOW modes
 */
@Service
public class TradingEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(TradingEngine.class);
    
    @Autowired
    private TradingBotConfig config;
    
    @Autowired
    private NeuralNetwork neuralNetwork;
    
    @Autowired
    private NeuralNetworkStrategy strategy;
    
    @Autowired
    private RiskManagement riskManagement;
    
    @Autowired
    private OrderManager orderManager;
    
    @Autowired
    private MarketDataFetcher marketDataFetcher;
    
    @Autowired
    private DataPreprocessor dataPreprocessor;
    
    @Autowired
    private TradingUpdateService updateService;
    
    @Autowired
    private BotConfigRepository botConfigRepository;
    
    @Autowired
    private PortfolioRepository portfolioRepository;
    
    @Autowired
    private TradingHistoryRepository tradingHistoryRepository;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private BotConfig currentConfig;
    private LocalDateTime lastTradeTime;
    
    @PostConstruct
    public void initialize() {
        try {
            loadConfiguration();
            initializePortfolio();
            logger.info("Trading engine initialized in {} mode", currentConfig.getMode());
        } catch (Exception e) {
            logger.error("Failed to initialize trading engine", e);
        }
    }
    
    /**
     * Main trading loop - executes every 30 seconds by default
     */
    @Scheduled(fixedDelayString = "${app.scheduler.trading-interval:30000}")
    public void executeTradingCycle() {
        if (!isRunning.get() || isPaused.get()) {
            return;
        }
        
        try {
            logger.debug("Starting trading cycle");
            
            // Check if market data is available
            if (!marketDataFetcher.isMarketDataAvailable()) {
                logger.warn("Market data not available, skipping trading cycle");
                return;
            }
            
            // Execute trading for each configured pair
            for (String tradingPair : config.getTradingPairs()) {
                executeTradingForPair(tradingPair);
            }
            
            // Update portfolio status
            updatePortfolioStatus();
            
            logger.debug("Trading cycle completed");
            
        } catch (Exception e) {
            logger.error("Error in trading cycle", e);
            updateService.sendBotStatusUpdate("ERROR", currentConfig.getMode().toString(), 
                Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Execute trading logic for a specific trading pair
     */
    private void executeTradingForPair(String tradingPair) {
        try {
            // Get current market data
            BigDecimal currentPrice = marketDataFetcher.getCurrentPrice(tradingPair);
            if (currentPrice == null) {
                logger.warn("No price data available for {}", tradingPair);
                return;
            }
            
            // Update historical data for ML
            BigDecimal volume = marketDataFetcher.getVolume(tradingPair);
            dataPreprocessor.updateHistoricalData(tradingPair, currentPrice, volume);
            
            // Prepare input features for neural network
            double[] inputFeatures = dataPreprocessor.prepareInputFeatures(tradingPair);
            
            // Get ML prediction
            NeuralNetwork.TradingPrediction prediction = neuralNetwork.predict(inputFeatures);
            
            // Send ML prediction update
            updateService.sendMLPredictionUpdate(
                tradingPair,
                String.format("%.4f", prediction.getPriceDirection()),
                String.format("%.2f", prediction.getConfidence() * 100),
                prediction.isPredictingUp() ? "UP" : "DOWN"
            );
            
            // Execute strategy based on prediction
            TradingDecision decision = strategy.makeDecision(tradingPair, prediction, currentPrice);
            
            if (decision.shouldTrade()) {
                executeTradingDecision(tradingPair, decision, currentPrice);
            }
            
        } catch (Exception e) {
            logger.error("Error executing trading for pair {}", tradingPair, e);
        }
    }
    
    /**
     * Execute a trading decision
     */
    private void executeTradingDecision(String tradingPair, TradingDecision decision, BigDecimal currentPrice) {
        try {
            // Risk management check
            if (!riskManagement.isTradeAllowed(decision, getCurrentBalance())) {
                logger.info("Trade rejected by risk management for {}", tradingPair);
                return;
            }
            
            // Calculate position size
            BigDecimal positionSize = riskManagement.calculatePositionSize(
                decision.getAmount(), getCurrentBalance());
            
            // Execute order based on mode
            OrderResult result;
            if (currentConfig.getMode() == BotConfig.TradingMode.PRODUCTION) {
                result = orderManager.executeRealOrder(tradingPair, decision.getAction(), 
                                                     positionSize, currentPrice);
            } else {
                result = orderManager.executeVirtualOrder(tradingPair, decision.getAction(), 
                                                        positionSize, currentPrice);
            }
            
            if (result.isSuccessful()) {
                // Record trade in history
                recordTrade(tradingPair, decision.getAction(), positionSize, currentPrice, result);
                
                // Update last trade time
                lastTradeTime = LocalDateTime.now();
                
                // Send trade execution update
                updateService.sendTradeExecutionUpdate(
                    tradingPair,
                    decision.getAction().toString(),
                    positionSize.toString(),
                    currentPrice.toString(),
                    currentConfig.getMode() == BotConfig.TradingMode.SHADOW
                );
                
                logger.info("Trade executed: {} {} {} at {} ({})", 
                           decision.getAction(), positionSize, tradingPair, currentPrice,
                           currentConfig.getMode());
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute trading decision for {}", tradingPair, e);
        }
    }
    
    /**
     * Record trade in trading history
     */
    private void recordTrade(String tradingPair, TradingDecision.Action action, 
                           BigDecimal amount, BigDecimal price, OrderResult result) {
        try {
            TradingHistory trade = new TradingHistory(
                tradingPair,
                action == TradingDecision.Action.BUY ? TradingHistory.TradeAction.BUY : TradingHistory.TradeAction.SELL,
                amount,
                price,
                getCurrentBalance(),
                currentConfig.getMode() == BotConfig.TradingMode.SHADOW
            );
            
            trade.setStrategyUsed("NeuralNetwork");
            trade.setMlConfidence(result.getConfidence());
            
            tradingHistoryRepository.save(trade);
            
        } catch (Exception e) {
            logger.error("Failed to record trade in history", e);
        }
    }
    
    /**
     * Train neural network with recent trading results
     */
    @Scheduled(fixedDelayString = "${app.scheduler.ml-training-interval:300000}")
    public void trainNeuralNetwork() {
        if (!isRunning.get() || !neuralNetwork.isInitialized()) {
            return;
        }
        
        try {
            // Get recent trades for training
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
            List<TradingHistory> recentTrades = tradingHistoryRepository.findRecentTrades(cutoffTime);
            
            if (recentTrades.isEmpty()) {
                logger.debug("No recent trades for neural network training");
                return;
            }
            
            // Convert trades to training results
            for (TradingHistory trade : recentTrades) {
                TradingResult result = createTradingResult(trade);
                neuralNetwork.trainWithResult(result);
            }
            
            logger.info("Neural network trained with {} recent trades", recentTrades.size());
            
        } catch (Exception e) {
            logger.error("Failed to train neural network", e);
        }
    }
    
    /**
     * Create TradingResult from TradingHistory for ML training
     */
    private TradingResult createTradingResult(TradingHistory trade) {
        // Prepare input features (simplified - in production would use actual features)
        double[] inputFeatures = dataPreprocessor.prepareInputFeatures(trade.getTradingPair());
        
        // Calculate metrics
        double currentBalance = getCurrentBalance().doubleValue();
        double initialBalance = config.getVirtualBalance().doubleValue();
        
        // Get prediction accuracy stats
        int totalPredictions = (int) tradingHistoryRepository.count();
        int correctPredictions = totalPredictions / 2; // Simplified calculation
        
        // Calculate time since last trade
        double timeSinceLastTrade = lastTradeTime != null ? 
            ChronoUnit.HOURS.between(lastTradeTime, trade.getTradeTime()) : 1.0;
        
        // Determine if trade was successful
        boolean successful = trade.getProfitLoss() != null && 
                           trade.getProfitLoss().compareTo(BigDecimal.ZERO) > 0;
        
        double profitLoss = trade.getProfitLoss() != null ? 
                          trade.getProfitLoss().doubleValue() : 0.0;
        
        return new TradingResult(
            inputFeatures,
            currentBalance,
            initialBalance,
            correctPredictions,
            totalPredictions,
            timeSinceLastTrade,
            profitLoss,
            successful
        );
    }
    
    /**
     * Get current balance based on mode
     */
    private BigDecimal getCurrentBalance() {
        if (currentConfig.getMode() == BotConfig.TradingMode.SHADOW) {
            Portfolio virtualPortfolio = portfolioRepository
                .findByCurrencyAndIsVirtual("USDT", true)
                .orElse(null);
            return virtualPortfolio != null ? virtualPortfolio.getBalance() : config.getVirtualBalance();
        } else {
            // In production mode, would get real balance from exchange
            return BigDecimal.valueOf(1000); // Placeholder
        }
    }
    
    /**
     * Update portfolio status and send updates
     */
    private void updatePortfolioStatus() {
        try {
            BigDecimal currentBalance = getCurrentBalance();
            BigDecimal initialBalance = config.getVirtualBalance();
            BigDecimal profitLoss = currentBalance.subtract(initialBalance);
            BigDecimal profitLossPercent = profitLoss.divide(initialBalance, 4, RoundingMode.HALF_UP)
                                                   .multiply(BigDecimal.valueOf(100));
            
            updateService.sendPortfolioUpdate(Map.of(
                "balance", currentBalance.toString(),
                "profitLoss", profitLoss.toString(),
                "profitLossPercent", profitLossPercent.toString(),
                "mode", currentConfig.getMode().toString(),
                "lastUpdate", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to update portfolio status", e);
        }
    }
    
    /**
     * Load bot configuration
     */
    private void loadConfiguration() {
        currentConfig = botConfigRepository.findCurrentConfig()
            .orElse(new BotConfig(BotConfig.TradingMode.SHADOW, config.getVirtualBalance()));
        
        if (currentConfig.getId() == null) {
            currentConfig = botConfigRepository.save(currentConfig);
        }
    }
    
    /**
     * Initialize portfolio for shadow mode
     */
    private void initializePortfolio() {
        if (currentConfig.getMode() == BotConfig.TradingMode.SHADOW) {
            Portfolio virtualPortfolio = portfolioRepository
                .findByCurrencyAndIsVirtual("USDT", true)
                .orElse(new Portfolio("USDT", config.getVirtualBalance(), true));
            
            if (virtualPortfolio.getId() == null) {
                portfolioRepository.save(virtualPortfolio);
                logger.info("Initialized virtual portfolio with {} USDT", config.getVirtualBalance());
            }
        }
    }
    
    // Control methods
    public void start() {
        isRunning.set(true);
        isPaused.set(false);
        updateService.sendBotStatusUpdate("RUNNING", currentConfig.getMode().toString(), 
            Map.of("startTime", LocalDateTime.now()));
        logger.info("Trading engine started");
    }
    
    public void stop() {
        isRunning.set(false);
        updateService.sendBotStatusUpdate("STOPPED", currentConfig.getMode().toString(), 
            Map.of("stopTime", LocalDateTime.now()));
        logger.info("Trading engine stopped");
    }
    
    public void pause() {
        isPaused.set(true);
        updateService.sendBotStatusUpdate("PAUSED", currentConfig.getMode().toString(), 
            Map.of("pauseTime", LocalDateTime.now()));
        logger.info("Trading engine paused");
    }
    
    public void resume() {
        isPaused.set(false);
        updateService.sendBotStatusUpdate("RUNNING", currentConfig.getMode().toString(), 
            Map.of("resumeTime", LocalDateTime.now()));
        logger.info("Trading engine resumed");
    }
    
    public boolean isRunning() {
        return isRunning.get();
    }
    
    public boolean isPaused() {
        return isPaused.get();
    }
    
    public BotConfig.TradingMode getCurrentMode() {
        return currentConfig.getMode();
    }
    
    /**
     * Switch trading mode
     */
    public void switchMode(BotConfig.TradingMode newMode) {
        BotConfig.TradingMode oldMode = currentConfig.getMode();
        currentConfig.setMode(newMode);
        botConfigRepository.save(currentConfig);
        
        if (newMode == BotConfig.TradingMode.SHADOW) {
            initializePortfolio();
        }
        
        updateService.sendBotStatusUpdate("MODE_CHANGED", newMode.toString(), 
            Map.of("oldMode", oldMode.toString(), "newMode", newMode.toString()));
        
        logger.info("Trading mode switched from {} to {}", oldMode, newMode);
    }
}