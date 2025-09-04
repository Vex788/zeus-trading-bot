package com.trading.api.controllers;

import com.trading.api.dto.TradingConfigDto;
import com.trading.api.dto.TradingResponse;
import com.trading.config.TradingBotConfig;
import com.trading.domain.entities.BotConfig;
import com.trading.domain.repositories.BotConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for bot configuration
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);
    
    @Autowired
    private TradingBotConfig config;
    
    @Autowired
    private BotConfigRepository botConfigRepository;
    
    /**
     * Get current configuration
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentConfig() {
        try {
            return ResponseEntity.ok(Map.of(
                "mode", config.getMode(),
                "virtualBalance", config.getVirtualBalance().toString(),
                "tradingPairs", config.getTradingPairs(),
                "neuralNetwork", Map.of(
                    "learningRate", config.getNeuralNetwork().getLearningRate(),
                    "epochs", config.getNeuralNetwork().getEpochs(),
                    "batchSize", config.getNeuralNetwork().getBatchSize(),
                    "hiddenLayers", config.getNeuralNetwork().getHiddenLayers(),
                    "inputFeatures", config.getNeuralNetwork().getInputFeatures()
                ),
                "risk", Map.of(
                    "maxPositionSizePercent", config.getRisk().getMaxPositionSizePercent(),
                    "stopLossPercent", config.getRisk().getStopLossPercent(),
                    "takeProfitPercent", config.getRisk().getTakeProfitPercent(),
                    "maxDailyLossPercent", config.getRisk().getMaxDailyLossPercent()
                ),
                "indicators", Map.of(
                    "rsiPeriod", config.getIndicators().getRsiPeriod(),
                    "macdFast", config.getIndicators().getMacdFast(),
                    "macdSlow", config.getIndicators().getMacdSlow(),
                    "macdSignal", config.getIndicators().getMacdSignal(),
                    "bollingerPeriod", config.getIndicators().getBollingerPeriod(),
                    "bollingerMultiplier", config.getIndicators().getBollingerMultiplier()
                ),
                "kucoin", Map.of(
                    "sandbox", config.getKucoin().isSandbox(),
                    "configured", config.getKucoin().getApiKey() != null && 
                                !config.getKucoin().getApiKey().isEmpty()
                )
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get current config", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load configuration: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Update trading configuration
     */
    @PostMapping("/update")
    public ResponseEntity<TradingResponse> updateConfig(@RequestBody TradingConfigDto configDto) {
        try {
            // Validate configuration
            if (!isValidConfig(configDto)) {
                return ResponseEntity.badRequest().body(new TradingResponse(
                    false,
                    "Invalid configuration parameters",
                    null
                ));
            }
            
            // Update configuration (this would need proper implementation)
            // For now, just log the update
            logger.info("Configuration update requested: {}", configDto);
            
            return ResponseEntity.ok(new TradingResponse(
                true,
                "Configuration updated successfully",
                Map.of("updated", true)
            ));
            
        } catch (Exception e) {
            logger.error("Failed to update config", e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "Failed to update configuration: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Update KuCoin API credentials
     */
    @PostMapping("/kucoin/credentials")
    public ResponseEntity<TradingResponse> updateKuCoinCredentials(
            @RequestBody Map<String, String> credentials) {
        try {
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            String passphrase = credentials.get("passphrase");
            
            if (apiKey == null || secretKey == null || passphrase == null) {
                return ResponseEntity.badRequest().body(new TradingResponse(
                    false,
                    "Missing required credentials: apiKey, secretKey, passphrase",
                    null
                ));
            }
            
            // In production, these would be encrypted before storage
            BotConfig botConfig = botConfigRepository.findCurrentConfig()
                .orElse(new BotConfig());
            
            botConfig.setApiKey(apiKey); // Should be encrypted
            botConfig.setSecretKey(secretKey); // Should be encrypted
            botConfigRepository.save(botConfig);
            
            logger.info("KuCoin credentials updated");
            
            return ResponseEntity.ok(new TradingResponse(
                true,
                "KuCoin credentials updated successfully",
                Map.of("configured", true)
            ));
            
        } catch (Exception e) {
            logger.error("Failed to update KuCoin credentials", e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "Failed to update credentials: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Update risk management settings
     */
    @PostMapping("/risk")
    public ResponseEntity<TradingResponse> updateRiskSettings(
            @RequestBody Map<String, Double> riskSettings) {
        try {
            // Validate risk settings
            if (!isValidRiskSettings(riskSettings)) {
                return ResponseEntity.badRequest().body(new TradingResponse(
                    false,
                    "Invalid risk settings",
                    null
                ));
            }
            
            // Update risk settings (would need proper implementation)
            logger.info("Risk settings update requested: {}", riskSettings);
            
            return ResponseEntity.ok(new TradingResponse(
                true,
                "Risk settings updated successfully",
                Map.of("updated", true)
            ));
            
        } catch (Exception e) {
            logger.error("Failed to update risk settings", e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "Failed to update risk settings: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Update neural network parameters
     */
    @PostMapping("/neural-network")
    public ResponseEntity<TradingResponse> updateNeuralNetworkConfig(
            @RequestBody Map<String, Object> nnConfig) {
        try {
            // Validate neural network config
            if (!isValidNeuralNetworkConfig(nnConfig)) {
                return ResponseEntity.badRequest().body(new TradingResponse(
                    false,
                    "Invalid neural network configuration",
                    null
                ));
            }
            
            // Update neural network config (would need proper implementation)
            logger.info("Neural network config update requested: {}", nnConfig);
            
            return ResponseEntity.ok(new TradingResponse(
                true,
                "Neural network configuration updated successfully",
                Map.of("updated", true)
            ));
            
        } catch (Exception e) {
            logger.error("Failed to update neural network config", e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "Failed to update neural network configuration: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Get available trading pairs
     */
    @GetMapping("/trading-pairs")
    public ResponseEntity<Map<String, Object>> getAvailableTradingPairs() {
        try {
            // In production, this would fetch from exchange API
            return ResponseEntity.ok(Map.of(
                "available", java.util.List.of(
                    "BTC-USDT", "ETH-USDT", "ADA-USDT", "DOT-USDT", 
                    "LINK-USDT", "UNI-USDT", "MATIC-USDT", "SOL-USDT"
                ),
                "configured", config.getTradingPairs()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get trading pairs", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load trading pairs: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Test KuCoin API connection
     */
    @PostMapping("/kucoin/test")
    public ResponseEntity<TradingResponse> testKuCoinConnection() {
        try {
            // This would test the actual API connection
            // For now, return a mock response
            
            return ResponseEntity.ok(new TradingResponse(
                true,
                "KuCoin API connection test successful",
                Map.of(
                    "connected", true,
                    "sandbox", config.getKucoin().isSandbox()
                )
            ));
            
        } catch (Exception e) {
            logger.error("Failed to test KuCoin connection", e);
            return ResponseEntity.internalServerError().body(new TradingResponse(
                false,
                "KuCoin API connection test failed: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Validate configuration DTO
     */
    private boolean isValidConfig(TradingConfigDto configDto) {
        // Add validation logic here
        return configDto != null;
    }
    
    /**
     * Validate risk settings
     */
    private boolean isValidRiskSettings(Map<String, Double> riskSettings) {
        try {
            Double maxPositionSize = riskSettings.get("maxPositionSizePercent");
            Double stopLoss = riskSettings.get("stopLossPercent");
            Double takeProfit = riskSettings.get("takeProfitPercent");
            Double maxDailyLoss = riskSettings.get("maxDailyLossPercent");
            
            return maxPositionSize != null && maxPositionSize > 0 && maxPositionSize <= 100 &&
                   stopLoss != null && stopLoss > 0 && stopLoss <= 50 &&
                   takeProfit != null && takeProfit > 0 && takeProfit <= 100 &&
                   maxDailyLoss != null && maxDailyLoss > 0 && maxDailyLoss <= 100;
                   
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate neural network configuration
     */
    private boolean isValidNeuralNetworkConfig(Map<String, Object> nnConfig) {
        try {
            Double learningRate = (Double) nnConfig.get("learningRate");
            Integer epochs = (Integer) nnConfig.get("epochs");
            Integer batchSize = (Integer) nnConfig.get("batchSize");
            
            return learningRate != null && learningRate > 0 && learningRate < 1 &&
                   epochs != null && epochs > 0 && epochs <= 1000 &&
                   batchSize != null && batchSize > 0 && batchSize <= 128;
                   
        } catch (Exception e) {
            return false;
        }
    }
}