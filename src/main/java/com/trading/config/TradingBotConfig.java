package com.trading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configuration properties for the trading bot
 */
@Configuration
@ConfigurationProperties(prefix = "trading.bot")
public class TradingBotConfig {
    
    private String mode = "SHADOW";
    private BigDecimal virtualBalance = new BigDecimal("100.0");
    private KuCoinConfig kucoin = new KuCoinConfig();
    private NeuralNetworkConfig neuralNetwork = new NeuralNetworkConfig();
    private RiskConfig risk = new RiskConfig();
    private List<String> tradingPairs = List.of("BTC-USDT", "ETH-USDT");
    private IndicatorsConfig indicators = new IndicatorsConfig();
    
    // Getters and Setters
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    
    public BigDecimal getVirtualBalance() { return virtualBalance; }
    public void setVirtualBalance(BigDecimal virtualBalance) { this.virtualBalance = virtualBalance; }
    
    public KuCoinConfig getKucoin() { return kucoin; }
    public void setKucoin(KuCoinConfig kucoin) { this.kucoin = kucoin; }
    
    public NeuralNetworkConfig getNeuralNetwork() { return neuralNetwork; }
    public void setNeuralNetwork(NeuralNetworkConfig neuralNetwork) { this.neuralNetwork = neuralNetwork; }
    
    public RiskConfig getRisk() { return risk; }
    public void setRisk(RiskConfig risk) { this.risk = risk; }
    
    public List<String> getTradingPairs() { return tradingPairs; }
    public void setTradingPairs(List<String> tradingPairs) { this.tradingPairs = tradingPairs; }
    
    public IndicatorsConfig getIndicators() { return indicators; }
    public void setIndicators(IndicatorsConfig indicators) { this.indicators = indicators; }
    
    public static class KuCoinConfig {
        private String apiKey;
        private String secretKey;
        private String passphrase;
        private boolean sandbox = true;
        
        // Getters and Setters
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        
        public String getPassphrase() { return passphrase; }
        public void setPassphrase(String passphrase) { this.passphrase = passphrase; }
        
        public boolean isSandbox() { return sandbox; }
        public void setSandbox(boolean sandbox) { this.sandbox = sandbox; }
    }
    
    public static class NeuralNetworkConfig {
        private double learningRate = 0.001;
        private int epochs = 100;
        private int batchSize = 32;
        private int[] hiddenLayers = {64, 32, 16};
        private int inputFeatures = 20;
        
        // Getters and Setters
        public double getLearningRate() { return learningRate; }
        public void setLearningRate(double learningRate) { this.learningRate = learningRate; }
        
        public int getEpochs() { return epochs; }
        public void setEpochs(int epochs) { this.epochs = epochs; }
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        
        public int[] getHiddenLayers() { return hiddenLayers; }
        public void setHiddenLayers(int[] hiddenLayers) { this.hiddenLayers = hiddenLayers; }
        
        public int getInputFeatures() { return inputFeatures; }
        public void setInputFeatures(int inputFeatures) { this.inputFeatures = inputFeatures; }
    }
    
    public static class RiskConfig {
        private double maxPositionSizePercent = 10.0;
        private double stopLossPercent = 5.0;
        private double takeProfitPercent = 15.0;
        private double maxDailyLossPercent = 20.0;
        
        // Getters and Setters
        public double getMaxPositionSizePercent() { return maxPositionSizePercent; }
        public void setMaxPositionSizePercent(double maxPositionSizePercent) { this.maxPositionSizePercent = maxPositionSizePercent; }
        
        public double getStopLossPercent() { return stopLossPercent; }
        public void setStopLossPercent(double stopLossPercent) { this.stopLossPercent = stopLossPercent; }
        
        public double getTakeProfitPercent() { return takeProfitPercent; }
        public void setTakeProfitPercent(double takeProfitPercent) { this.takeProfitPercent = takeProfitPercent; }
        
        public double getMaxDailyLossPercent() { return maxDailyLossPercent; }
        public void setMaxDailyLossPercent(double maxDailyLossPercent) { this.maxDailyLossPercent = maxDailyLossPercent; }
    }
    
    public static class IndicatorsConfig {
        private int rsiPeriod = 14;
        private int macdFast = 12;
        private int macdSlow = 26;
        private int macdSignal = 9;
        private int bollingerPeriod = 20;
        private double bollingerMultiplier = 2.0;
        
        // Getters and Setters
        public int getRsiPeriod() { return rsiPeriod; }
        public void setRsiPeriod(int rsiPeriod) { this.rsiPeriod = rsiPeriod; }
        
        public int getMacdFast() { return macdFast; }
        public void setMacdFast(int macdFast) { this.macdFast = macdFast; }
        
        public int getMacdSlow() { return macdSlow; }
        public void setMacdSlow(int macdSlow) { this.macdSlow = macdSlow; }
        
        public int getMacdSignal() { return macdSignal; }
        public void setMacdSignal(int macdSignal) { this.macdSignal = macdSignal; }
        
        public int getBollingerPeriod() { return bollingerPeriod; }
        public void setBollingerPeriod(int bollingerPeriod) { this.bollingerPeriod = bollingerPeriod; }
        
        public double getBollingerMultiplier() { return bollingerMultiplier; }
        public void setBollingerMultiplier(double bollingerMultiplier) { this.bollingerMultiplier = bollingerMultiplier; }
    }
}