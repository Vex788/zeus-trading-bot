package com.trading.core.ml;

import com.trading.config.TradingBotConfig;
import com.trading.integration.kucoin.MarketDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data preprocessor for preparing market data for neural network input
 */
@Component
public class DataPreprocessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DataPreprocessor.class);
    
    @Autowired
    private TradingBotConfig config;
    
    @Autowired
    private MarketDataFetcher marketDataFetcher;
    
    // Historical data storage (in production, this would be from a database)
    private final Map<String, List<MarketDataPoint>> historicalData = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * Prepare input features for neural network prediction
     */
    public double[] prepareInputFeatures(String tradingPair) {
        try {
            List<MarketDataPoint> history = getHistoricalData(tradingPair);
            if (history.size() < 50) { // Need minimum data for indicators
                logger.warn("Insufficient historical data for {}: {} points", tradingPair, history.size());
                return createDefaultFeatures();
            }
            
            BarSeries series = createBarSeries(history);
            return extractFeatures(series);
            
        } catch (Exception e) {
            logger.error("Failed to prepare input features for {}", tradingPair, e);
            return createDefaultFeatures();
        }
    }
    
    /**
     * Update historical data with current market data
     */
    public void updateHistoricalData(String tradingPair, BigDecimal price, BigDecimal volume) {
        List<MarketDataPoint> history = historicalData.computeIfAbsent(tradingPair, k -> new ArrayList<>());
        
        MarketDataPoint dataPoint = new MarketDataPoint(
            LocalDateTime.now(),
            price.doubleValue(),
            price.doubleValue(), // Using same price for OHLC (simplified)
            price.doubleValue(),
            price.doubleValue(),
            volume.doubleValue()
        );
        
        history.add(dataPoint);
        
        // Keep only last 200 data points to manage memory
        if (history.size() > 200) {
            history.remove(0);
        }
        
        logger.debug("Updated historical data for {}: {} points", tradingPair, history.size());
    }
    
    /**
     * Extract technical indicators as features
     */
    private double[] extractFeatures(BarSeries series) {
        TradingBotConfig.IndicatorsConfig indicators = config.getIndicators();
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        
        // RSI
        RSIIndicator rsi = new RSIIndicator(closePrice, indicators.getRsiPeriod());
        
        // MACD
        MACDIndicator macd = new MACDIndicator(closePrice, indicators.getMacdFast(), indicators.getMacdSlow());
        
        // Bollinger Bands
        SMAIndicator sma = new SMAIndicator(closePrice, indicators.getBollingerPeriod());
        BollingerBandsMiddleIndicator bbMiddle = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator bbUpper = new BollingerBandsUpperIndicator(bbMiddle, closePrice, indicators.getBollingerMultiplier());
        BollingerBandsLowerIndicator bbLower = new BollingerBandsLowerIndicator(bbMiddle, closePrice, indicators.getBollingerMultiplier());
        
        int lastIndex = series.getEndIndex();
        
        // Extract feature values
        double[] features = new double[20]; // 20 input features as configured
        int idx = 0;
        
        // Price-based features (normalized)
        double currentPrice = closePrice.getValue(lastIndex).doubleValue();
        double previousPrice = lastIndex > 0 ? closePrice.getValue(lastIndex - 1).doubleValue() : currentPrice;
        features[idx++] = normalize(currentPrice / previousPrice - 1, -0.1, 0.1); // Price change
        
        // Volume features
        double currentVolume = volume.getValue(lastIndex).doubleValue();
        double avgVolume = calculateAverageVolume(series, 10);
        features[idx++] = normalize(currentVolume / avgVolume - 1, -2, 2); // Volume ratio
        
        // RSI
        features[idx++] = normalize(rsi.getValue(lastIndex).doubleValue(), 0, 100);
        
        // MACD
        features[idx++] = normalize(macd.getValue(lastIndex).doubleValue(), -1, 1);
        
        // Bollinger Bands position
        double bbPosition = (currentPrice - bbLower.getValue(lastIndex).doubleValue()) / 
                           (bbUpper.getValue(lastIndex).doubleValue() - bbLower.getValue(lastIndex).doubleValue());
        features[idx++] = normalize(bbPosition, 0, 1);
        
        // Moving averages
        SMAIndicator sma5 = new SMAIndicator(closePrice, 5);
        SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        
        features[idx++] = normalize(currentPrice / sma5.getValue(lastIndex).doubleValue() - 1, -0.1, 0.1);
        features[idx++] = normalize(currentPrice / sma10.getValue(lastIndex).doubleValue() - 1, -0.1, 0.1);
        features[idx++] = normalize(currentPrice / sma20.getValue(lastIndex).doubleValue() - 1, -0.1, 0.1);
        
        // Price momentum features
        if (lastIndex >= 5) {
            double momentum5 = currentPrice / closePrice.getValue(lastIndex - 5).doubleValue() - 1;
            features[idx++] = normalize(momentum5, -0.2, 0.2);
        } else {
            features[idx++] = 0.5;
        }
        
        if (lastIndex >= 10) {
            double momentum10 = currentPrice / closePrice.getValue(lastIndex - 10).doubleValue() - 1;
            features[idx++] = normalize(momentum10, -0.3, 0.3);
        } else {
            features[idx++] = 0.5;
        }
        
        // Volatility features
        double volatility = calculateVolatility(series, 10);
        features[idx++] = normalize(volatility, 0, 0.1);
        
        // Time-based features
        LocalDateTime now = LocalDateTime.now();
        features[idx++] = normalize(now.getHour(), 0, 23); // Hour of day
        features[idx++] = normalize(now.getDayOfWeek().getValue(), 1, 7); // Day of week
        
        // Fill remaining features with trend indicators
        while (idx < features.length) {
            features[idx++] = 0.5; // Neutral value
        }
        
        return features;
    }
    
    /**
     * Normalize value to 0-1 range
     */
    private double normalize(double value, double min, double max) {
        if (max == min) return 0.5;
        double normalized = (value - min) / (max - min);
        return Math.max(0, Math.min(1, normalized));
    }
    
    /**
     * Calculate average volume over specified periods
     */
    private double calculateAverageVolume(BarSeries series, int periods) {
        VolumeIndicator volume = new VolumeIndicator(series);
        double sum = 0;
        int count = 0;
        int endIndex = series.getEndIndex();
        
        for (int i = Math.max(0, endIndex - periods + 1); i <= endIndex; i++) {
            sum += volume.getValue(i).doubleValue();
            count++;
        }
        
        return count > 0 ? sum / count : 1.0;
    }
    
    /**
     * Calculate price volatility
     */
    private double calculateVolatility(BarSeries series, int periods) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        int endIndex = series.getEndIndex();
        
        if (endIndex < periods) return 0.01; // Default low volatility
        
        double sum = 0;
        double sumSquares = 0;
        int count = 0;
        
        for (int i = endIndex - periods + 1; i <= endIndex; i++) {
            if (i > 0) {
                double change = closePrice.getValue(i).doubleValue() / closePrice.getValue(i-1).doubleValue() - 1;
                sum += change;
                sumSquares += change * change;
                count++;
            }
        }
        
        if (count <= 1) return 0.01;
        
        double mean = sum / count;
        double variance = (sumSquares / count) - (mean * mean);
        return Math.sqrt(variance);
    }
    
    /**
     * Create BarSeries from historical data
     */
    private BarSeries createBarSeries(List<MarketDataPoint> history) {
        BarSeries series = new BaseBarSeries("Market Data");
        
        for (MarketDataPoint point : history) {
            ZonedDateTime time = point.getTimestamp().atZone(ZoneOffset.UTC);
            series.addBar(time, 
                         DecimalNum.valueOf(point.getOpen()),
                         DecimalNum.valueOf(point.getHigh()),
                         DecimalNum.valueOf(point.getLow()),
                         DecimalNum.valueOf(point.getClose()),
                         DecimalNum.valueOf(point.getVolume()));
        }
        
        return series;
    }
    
    /**
     * Get historical data for a trading pair
     */
    private List<MarketDataPoint> getHistoricalData(String tradingPair) {
        return historicalData.getOrDefault(tradingPair, new ArrayList<>());
    }
    
    /**
     * Create default features when insufficient data
     */
    private double[] createDefaultFeatures() {
        double[] features = new double[config.getNeuralNetwork().getInputFeatures()];
        for (int i = 0; i < features.length; i++) {
            features[i] = 0.5; // Neutral values
        }
        return features;
    }
    
    /**
     * Market data point for historical storage
     */
    public static class MarketDataPoint {
        private final LocalDateTime timestamp;
        private final double open;
        private final double high;
        private final double low;
        private final double close;
        private final double volume;
        
        public MarketDataPoint(LocalDateTime timestamp, double open, double high, 
                              double low, double close, double volume) {
            this.timestamp = timestamp;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public double getOpen() { return open; }
        public double getHigh() { return high; }
        public double getLow() { return low; }
        public double getClose() { return close; }
        public double getVolume() { return volume; }
    }
}