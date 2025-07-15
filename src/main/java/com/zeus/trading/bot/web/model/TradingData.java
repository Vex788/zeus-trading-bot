package com.zeus.trading.bot.web.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Data model for trading information to be displayed in the web interface.
 */
public class TradingData {

    private String currencyPair;
    private BigDecimal currentPrice;
    private ZonedDateTime timestamp;
    
    // Technical indicators
    private double rsiValue;
    private double macdValue;
    private double bollingerUpper;
    private double bollingerMiddle;
    private double bollingerLower;
    
    // Prediction
    private boolean predictedUp;
    
    // Open positions
    private List<PositionData> openPositions;
    
    // Historical prices for chart
    private List<PricePoint> priceHistory;

    // Getters and setters
    public String getCurrencyPair() {
        return currencyPair;
    }

    public void setCurrencyPair(String currencyPair) {
        this.currencyPair = currencyPair;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getRsiValue() {
        return rsiValue;
    }

    public void setRsiValue(double rsiValue) {
        this.rsiValue = rsiValue;
    }

    public double getMacdValue() {
        return macdValue;
    }

    public void setMacdValue(double macdValue) {
        this.macdValue = macdValue;
    }

    public double getBollingerUpper() {
        return bollingerUpper;
    }

    public void setBollingerUpper(double bollingerUpper) {
        this.bollingerUpper = bollingerUpper;
    }

    public double getBollingerMiddle() {
        return bollingerMiddle;
    }

    public void setBollingerMiddle(double bollingerMiddle) {
        this.bollingerMiddle = bollingerMiddle;
    }

    public double getBollingerLower() {
        return bollingerLower;
    }

    public void setBollingerLower(double bollingerLower) {
        this.bollingerLower = bollingerLower;
    }

    public boolean isPredictedUp() {
        return predictedUp;
    }

    public void setPredictedUp(boolean predictedUp) {
        this.predictedUp = predictedUp;
    }

    public List<PositionData> getOpenPositions() {
        return openPositions;
    }

    public void setOpenPositions(List<PositionData> openPositions) {
        this.openPositions = openPositions;
    }

    public List<PricePoint> getPriceHistory() {
        return priceHistory;
    }

    public void setPriceHistory(List<PricePoint> priceHistory) {
        this.priceHistory = priceHistory;
    }

    /**
     * Represents a position in the trading system.
     */
    public static class PositionData {
        private String id;
        private String currencyPair;
        private BigDecimal amount;
        private BigDecimal entryPrice;
        private ZonedDateTime entryDate;
        private BigDecimal currentPrice;
        private BigDecimal profitLoss;
        private String status;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCurrencyPair() {
            return currencyPair;
        }

        public void setCurrencyPair(String currencyPair) {
            this.currencyPair = currencyPair;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getEntryPrice() {
            return entryPrice;
        }

        public void setEntryPrice(BigDecimal entryPrice) {
            this.entryPrice = entryPrice;
        }

        public ZonedDateTime getEntryDate() {
            return entryDate;
        }

        public void setEntryDate(ZonedDateTime entryDate) {
            this.entryDate = entryDate;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
        }

        public BigDecimal getProfitLoss() {
            return profitLoss;
        }

        public void setProfitLoss(BigDecimal profitLoss) {
            this.profitLoss = profitLoss;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * Represents a price point for charting.
     */
    public static class PricePoint {
        private ZonedDateTime timestamp;
        private BigDecimal price;

        public PricePoint(ZonedDateTime timestamp, BigDecimal price) {
            this.timestamp = timestamp;
            this.price = price;
        }

        public ZonedDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(ZonedDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}