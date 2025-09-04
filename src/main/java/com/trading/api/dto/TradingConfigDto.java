package com.trading.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for trading configuration updates
 */
public class TradingConfigDto {
    
    private String mode;
    private BigDecimal virtualBalance;
    private List<String> tradingPairs;
    private NeuralNetworkConfigDto neuralNetwork;
    private RiskConfigDto risk;
    private IndicatorsConfigDto indicators;
    
    // Getters and Setters
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    
    public BigDecimal getVirtualBalance() { return virtualBalance; }
    public void setVirtualBalance(BigDecimal virtualBalance) { this.virtualBalance = virtualBalance; }
    
    public List<String> getTradingPairs() { return tradingPairs; }
    public void setTradingPairs(List<String> tradingPairs) { this.tradingPairs = tradingPairs; }
    
    public NeuralNetworkConfigDto getNeuralNetwork() { return neuralNetwork; }
    public void setNeuralNetwork(NeuralNetworkConfigDto neuralNetwork) { this.neuralNetwork = neuralNetwork; }
    
    public RiskConfigDto getRisk() { return risk; }
    public void setRisk(RiskConfigDto risk) { this.risk = risk; }
    
    public IndicatorsConfigDto getIndicators() { return indicators; }
    public void setIndicators(IndicatorsConfigDto indicators) { this.indicators = indicators; }
    
    public static class NeuralNetworkConfigDto {
        private double learningRate;
        private int epochs;
        private int batchSize;
        private int[] hiddenLayers;
        private int inputFeatures;
        
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
    
    public static class RiskConfigDto {
        private double maxPositionSizePercent;
        private double stopLossPercent;
        private double takeProfitPercent;
        private double maxDailyLossPercent;
        
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
    
    public static class IndicatorsConfigDto {
        private int rsiPeriod;
        private int macdFast;
        private int macdSlow;
        private int macdSignal;
        private int bollingerPeriod;
        private double bollingerMultiplier;
        
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