package com.trading.core.ml;

import java.math.BigDecimal;

/**
 * Trading result data for neural network training
 */
public class TradingResult {
    
    private final double[] inputFeatures;
    private final double currentBalance;
    private final double initialBalance;
    private final int correctPredictions;
    private final int totalPredictions;
    private final double timeSinceLastTrade; // in hours
    private final double profitLoss;
    private final boolean successful;
    
    public TradingResult(double[] inputFeatures, double currentBalance, double initialBalance,
                        int correctPredictions, int totalPredictions, double timeSinceLastTrade,
                        double profitLoss, boolean successful) {
        this.inputFeatures = inputFeatures;
        this.currentBalance = currentBalance;
        this.initialBalance = initialBalance;
        this.correctPredictions = correctPredictions;
        this.totalPredictions = totalPredictions;
        this.timeSinceLastTrade = timeSinceLastTrade;
        this.profitLoss = profitLoss;
        this.successful = successful;
    }
    
    // Getters
    public double[] getInputFeatures() { return inputFeatures; }
    public double getCurrentBalance() { return currentBalance; }
    public double getInitialBalance() { return initialBalance; }
    public int getCorrectPredictions() { return correctPredictions; }
    public int getTotalPredictions() { return totalPredictions; }
    public double getTimeSinceLastTrade() { return timeSinceLastTrade; }
    public double getProfitLoss() { return profitLoss; }
    public boolean isSuccessful() { return successful; }
}