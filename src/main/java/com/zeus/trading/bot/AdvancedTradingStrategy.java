package com.zeus.trading.bot;

import com.zeus.trading.bot.web.TradingController;
import com.zeus.trading.bot.web.model.TradingData;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.ta4j.core.*;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.DecimalNum;
import tech.cassandre.trading.bot.dto.market.TickerDTO;
import tech.cassandre.trading.bot.dto.position.PositionCreationResultDTO;
import tech.cassandre.trading.bot.dto.position.PositionDTO;
import tech.cassandre.trading.bot.dto.position.PositionRulesDTO;
import tech.cassandre.trading.bot.dto.position.PositionStatusDTO;
import tech.cassandre.trading.bot.dto.user.AccountDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyPairDTO;
import tech.cassandre.trading.bot.strategy.BasicCassandreStrategy;
import tech.cassandre.trading.bot.strategy.CassandreStrategy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Advanced trading strategy using technical indicators (RSI, MACD, Bollinger Bands)
 * with self-learning capabilities.
 */
@CassandreStrategy
@Configuration
public class AdvancedTradingStrategy extends BasicCassandreStrategy {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Default amount for positions. */
    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("0.001");

    /** Default rules for positions. */
    private static final PositionRulesDTO DEFAULT_RULES = PositionRulesDTO.builder()
            .stopGainPercentage(5f)
            .stopLossPercentage(3f)
            .build();

    /** Configurable cryptocurrency pairs to trade. */
    @Value("${trading.bot.cryptocurrencies:BTC/USDT,ETH/USDT}")
    private String cryptocurrenciesConfig;

    /** Set of currency pairs to trade. */
    private Set<CurrencyPairDTO> currencyPairs = new HashSet<>();

    /** Map to store ticker history for each currency pair. */
    private final Map<CurrencyPairDTO, CircularFifoQueue<TickerDTO>> tickerHistoryMap = new ConcurrentHashMap<>();

    /** Map to store TA4J bar series for each currency pair. */
    private final Map<CurrencyPairDTO, BarSeries> barSeriesMap = new ConcurrentHashMap<>();

    /** Map to store prediction history for self-learning. */
    private final Map<CurrencyPairDTO, List<PredictionRecord>> predictionHistoryMap = new ConcurrentHashMap<>();

    /** Map to store trend learning parameters for each currency pair. */
    private final Map<CurrencyPairDTO, TrendLearningParameters> trendLearningMap = new ConcurrentHashMap<>();

    /** Number of tickers to keep in history. */
    private static final int TICKER_HISTORY_SIZE = 100;

    /** RSI period. */
    private static final int RSI_PERIOD = 14;

    /** MACD short period. */
    private static final int MACD_SHORT_PERIOD = 12;

    /** MACD long period. */
    private static final int MACD_LONG_PERIOD = 26;

    /** Bollinger Bands period. */
    private static final int BOLLINGER_PERIOD = 20;

    /** Bollinger Bands K multiplier. */
    private static final Num BOLLINGER_K = DecimalNum.valueOf(2.0);

    /** Trading controller for web interface. */
    public TradingController tradingController;

    /**
     * Sets the trading controller.
     * Using setter injection to avoid circular dependency.
     *
     * @param tradingController the trading controller
     */
    @Autowired
    public void setTradingController(TradingController tradingController) {
        this.tradingController = tradingController;
    }

    /**
     * Initialize the strategy.
     */
    @Override
    public void initialize() {
        // Parse the configured cryptocurrency pairs
        String[] pairs = cryptocurrenciesConfig.split(",");
        for (String pair : pairs) {
            String[] currencies = pair.trim().split("/");
            if (currencies.length == 2) {
                CurrencyPairDTO currencyPair = new CurrencyPairDTO(
                        CurrencyDTO.getInstance(currencies[0]),
                        CurrencyDTO.getInstance(currencies[1])
                );
                currencyPairs.add(currencyPair);
                tickerHistoryMap.put(currencyPair, new CircularFifoQueue<>(TICKER_HISTORY_SIZE));
                barSeriesMap.put(currencyPair, new BaseBarSeries(pair));
                predictionHistoryMap.put(currencyPair, new ArrayList<>());
                trendLearningMap.put(currencyPair, new TrendLearningParameters());
                logger.info("Added currency pair for trading: {}", currencyPair);
            }
        }
    }

    @Override
    public Set<CurrencyPairDTO> getRequestedCurrencyPairs() {
        return currencyPairs;
    }

    @Override
    public Optional<AccountDTO> getTradeAccount(Set<AccountDTO> accounts) {
        // We choose the account whose name is "trade"
        return accounts.stream()
                .filter(a -> "trade".equalsIgnoreCase(a.getName()))
                .findFirst();
    }

    @Override
    public void onTickersUpdates(final Map<CurrencyPairDTO, TickerDTO> tickers) {
        // Process each currency pair
        for (CurrencyPairDTO currencyPair : currencyPairs) {
            TickerDTO ticker = tickers.get(currencyPair);
            if (ticker != null) {
                processTickerUpdate(currencyPair, ticker);
            }
        }
    }

    /**
     * Process a ticker update for a specific currency pair.
     *
     * @param currencyPair the currency pair
     * @param ticker the ticker
     */
    private void processTickerUpdate(CurrencyPairDTO currencyPair, TickerDTO ticker) {
        CircularFifoQueue<TickerDTO> tickerHistory = tickerHistoryMap.get(currencyPair);
        BarSeries barSeries = barSeriesMap.get(currencyPair);

        // Add ticker to history if it's a new minute
        if (tickerHistory.isEmpty() ||
                ticker.getTimestamp().isEqual(tickerHistory.get(tickerHistory.size() - 1).getTimestamp().plus(Duration.ofMinutes(1))) ||
                ticker.getTimestamp().isAfter(tickerHistory.get(tickerHistory.size() - 1).getTimestamp().plus(Duration.ofMinutes(1)))) {

            tickerHistory.add(ticker);

            // Add bar to bar series
            ZonedDateTime endTime = ZonedDateTime.now(); // Use current time as TickerDTO doesn't have toZonedDateTime
            Bar bar = BaseBar.builder()
                    .timePeriod(Duration.ofMinutes(1))
                    .endTime(endTime)
                    .openPrice(DecimalNum.valueOf(ticker.getOpen().doubleValue()))
                    .highPrice(DecimalNum.valueOf(ticker.getHigh().doubleValue()))
                    .lowPrice(DecimalNum.valueOf(ticker.getLow().doubleValue()))
                    .closePrice(DecimalNum.valueOf(ticker.getLast().doubleValue()))
                    .volume(DecimalNum.valueOf(ticker.getVolume().doubleValue()))
                    .build();
            barSeries.addBar(bar);

            // Only analyze if we have enough data
            if (barSeries.getBarCount() > BOLLINGER_PERIOD) {
                analyzeMarketAndTrade(currencyPair, barSeries);
            }
        }
    }

    /**
     * Analyze the market using technical indicators and make trading decisions.
     *
     * @param currencyPair the currency pair
     * @param barSeries the bar series
     */
    private void analyzeMarketAndTrade(CurrencyPairDTO currencyPair, BarSeries barSeries) {
        // Calculate indicators
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);

        // RSI
        RSIIndicator rsi = new RSIIndicator(closePrice, RSI_PERIOD);

        // MACD
        MACDIndicator macd = new MACDIndicator(closePrice, MACD_SHORT_PERIOD, MACD_LONG_PERIOD);

        // Bollinger Bands
        SMAIndicator sma = new SMAIndicator(closePrice, BOLLINGER_PERIOD);
        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, BOLLINGER_PERIOD);
        BollingerBandsMiddleIndicator bbMiddle = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator bbUpper = new BollingerBandsUpperIndicator(bbMiddle, sd, BOLLINGER_K);
        BollingerBandsLowerIndicator bbLower = new BollingerBandsLowerIndicator(bbMiddle, sd, BOLLINGER_K);

        int lastIndex = barSeries.getEndIndex();

        // Get indicator values
        double rsiValue = rsi.getValue(lastIndex).doubleValue();
        double macdValue = macd.getValue(lastIndex).doubleValue();
        double bbUpperValue = bbUpper.getValue(lastIndex).doubleValue();
        double bbMiddleValue = bbMiddle.getValue(lastIndex).doubleValue();
        double bbLowerValue = bbLower.getValue(lastIndex).doubleValue();
        double currentPrice = closePrice.getValue(lastIndex).doubleValue();

        // Log indicator values
        logger.info("{} - RSI: {}, MACD: {}, BB Upper: {}, BB Middle: {}, BB Lower: {}, Price: {}",
                currencyPair, rsiValue, macdValue, bbUpperValue, bbMiddleValue, bbLowerValue, currentPrice);

        // Make prediction based on indicators and learned parameters
        boolean predictedUp = predictPriceMovement(currencyPair, rsiValue, macdValue, currentPrice, bbUpperValue, bbLowerValue, bbMiddleValue);

        // Record prediction for self-learning
        PredictionRecord prediction = new PredictionRecord(
                barSeries.getBar(lastIndex).getEndTime(),
                currentPrice,
                predictedUp
        );
        predictionHistoryMap.get(currencyPair).add(prediction);

        // Check previous predictions and learn from mistakes
        learnFromPreviousPredictions(currencyPair, currentPrice);

        // Send trading data to web interface
        if (tradingController != null) {
            sendTradingDataToWebInterface(currencyPair, barSeries, rsiValue, macdValue, 
                    bbUpperValue, bbMiddleValue, bbLowerValue, currentPrice, predictedUp);
        }

        // Make trading decision
        if (predictedUp && !hasOpenPositions(currencyPair)) {
            // Buy signal
            if (canBuy(currencyPair, DEFAULT_AMOUNT)) {
                PositionCreationResultDTO result = createLongPosition(currencyPair, DEFAULT_AMOUNT, DEFAULT_RULES);
                if (result.isSuccessful()) {
                    logger.info("Created long position for {} with amount {}", currencyPair, DEFAULT_AMOUNT);
                } else {
                    logger.error("Failed to create position: {}", result.getErrorMessage());
                }
            }
        } else if (!predictedUp && hasOpenPositions(currencyPair)) {
            // Consider closing positions if prediction is down
            // This is handled by stop loss/gain rules, but could be manually implemented
        }
    }

    /**
     * Predict price movement based on technical indicators and learned parameters.
     *
     * @param currencyPair the currency pair
     * @param rsi RSI value
     * @param macd MACD value
     * @param price current price
     * @param bbUpper Bollinger Band upper value
     * @param bbLower Bollinger Band lower value
     * @param bbMiddle Bollinger Band middle value
     * @return true if price is predicted to go up, false otherwise
     */
    private boolean predictPriceMovement(CurrencyPairDTO currencyPair, double rsi, double macd, double price, 
                                         double bbUpper, double bbLower, double bbMiddle) {

        // Get learned parameters for this currency pair
        TrendLearningParameters params = trendLearningMap.get(currencyPair);
        if (params == null) {
            // Fallback to default behavior if parameters aren't available
            return defaultPredictPriceMovement(rsi, macd, price, bbUpper, bbLower, bbMiddle);
        }

        // RSI signals with learned thresholds
        boolean rsiOversold = rsi < params.getRsiOversoldThreshold();
        boolean rsiOverbought = rsi > params.getRsiOverboughtThreshold();

        // MACD signals
        boolean macdPositive = macd > 0;

        // Bollinger Bands signals
        boolean priceBelowLower = price < bbLower;
        boolean priceAboveUpper = price > bbUpper;
        boolean priceAboveMiddle = price > bbMiddle;

        // Calculate weighted signals for uptrend
        double uptrendSignal = 0;
        uptrendSignal += (rsiOversold ? 1 : 0) * params.getRsiUptrendWeight();
        uptrendSignal += (macdPositive ? 1 : 0) * params.getMacdUptrendWeight();
        uptrendSignal += (priceBelowLower ? 1 : 0) * params.getBollingerUptrendWeight();

        // Calculate weighted signals for downtrend
        double downtrendSignal = 0;
        downtrendSignal += (rsiOverbought ? 1 : 0) * params.getRsiDowntrendWeight();
        downtrendSignal += (!macdPositive ? 1 : 0) * params.getMacdDowntrendWeight();
        downtrendSignal += (priceAboveUpper ? 1 : 0) * params.getBollingerDowntrendWeight();

        // Log the weighted signals for debugging
        logger.debug("Weighted signals for {}: Uptrend={}, Downtrend={}", 
                currencyPair, uptrendSignal, downtrendSignal);

        // Make prediction based on weighted signals
        if (uptrendSignal > downtrendSignal) {
            return true;  // Predict uptrend
        } else if (downtrendSignal > uptrendSignal) {
            return false; // Predict downtrend
        } else {
            // If signals are equal, use price relative to middle Bollinger Band as tiebreaker
            return priceAboveMiddle;
        }
    }

    /**
     * Default prediction method without learning parameters.
     * Used as fallback when learning parameters aren't available.
     */
    private boolean defaultPredictPriceMovement(double rsi, double macd, double price, 
                                               double bbUpper, double bbLower, double bbMiddle) {
        // RSI signals
        boolean rsiOversold = rsi < 30;
        boolean rsiOverbought = rsi > 70;

        // MACD signals
        boolean macdPositive = macd > 0;

        // Bollinger Bands signals
        boolean priceBelowLower = price < bbLower;
        boolean priceAboveUpper = price > bbUpper;
        boolean priceAboveMiddle = price > bbMiddle;

        // Combined signals for upward movement
        boolean strongBuySignal = (rsiOversold && macdPositive) || (priceBelowLower && macdPositive);
        boolean moderateBuySignal = (rsi < 40 && macdPositive) || (price < bbMiddle && macd > -0.5);

        // Combined signals for downward movement
        boolean strongSellSignal = (rsiOverbought && !macdPositive) || (priceAboveUpper && !macdPositive);
        boolean moderateSellSignal = (rsi > 60 && !macdPositive) || (priceAboveMiddle && macd < 0.5);

        // Final decision with priority to stronger signals
        if (strongBuySignal) return true;
        if (strongSellSignal) return false;
        if (moderateBuySignal) return true;
        if (moderateSellSignal) return false;

        // Default to trend following if no clear signals
        return priceAboveMiddle;
    }

    /**
     * Learn from previous predictions by comparing them with actual outcomes.
     *
     * @param currencyPair the currency pair
     * @param currentPrice the current price
     */
    private void learnFromPreviousPredictions(CurrencyPairDTO currencyPair, double currentPrice) {
        List<PredictionRecord> predictions = predictionHistoryMap.get(currencyPair);
        TrendLearningParameters learningParams = trendLearningMap.get(currencyPair);

        // Check predictions that are 24 hours old
        ZonedDateTime oneDayAgo = ZonedDateTime.now().minusHours(24);

        for (Iterator<PredictionRecord> iterator = predictions.iterator(); iterator.hasNext();) {
            PredictionRecord prediction = iterator.next();

            // If prediction is older than 24 hours, evaluate it
            if (prediction.getTimestamp().isBefore(oneDayAgo) && !prediction.isEvaluated()) {
                boolean actualUp = currentPrice > prediction.getPrice();
                boolean correctPrediction = prediction.isPredictedUp() == actualUp;

                // Adjust weights based on prediction correctness
                learningParams.adjustWeights(prediction.isPredictedUp(), correctPrediction);

                if (!correctPrediction) {
                    logger.info("Incorrect prediction detected: predicted {}, actual {}. Learning from mistake.",
                            prediction.isPredictedUp() ? "UP" : "DOWN", actualUp ? "UP" : "DOWN");

                    // Log the adjusted parameters for transparency
                    logger.info("Adjusted learning parameters for {}: RSI thresholds [oversold={}, overbought={}], " +
                            "Uptrend weights [RSI={}, MACD={}, BB={}], Downtrend weights [RSI={}, MACD={}, BB={}]",
                            currencyPair,
                            learningParams.getRsiOversoldThreshold(),
                            learningParams.getRsiOverboughtThreshold(),
                            learningParams.getRsiUptrendWeight(),
                            learningParams.getMacdUptrendWeight(),
                            learningParams.getBollingerUptrendWeight(),
                            learningParams.getRsiDowntrendWeight(),
                            learningParams.getMacdDowntrendWeight(),
                            learningParams.getBollingerDowntrendWeight());
                }

                prediction.setEvaluated(true);

                // Remove very old predictions (older than 7 days)
                if (prediction.getTimestamp().isBefore(ZonedDateTime.now().minusDays(7))) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Check if there are open positions for a currency pair.
     *
     * @param currencyPair the currency pair
     * @return true if there are open positions
     */
    private boolean hasOpenPositions(CurrencyPairDTO currencyPair) {
        // For now, we'll assume there are no open positions
        // In a real implementation, we would check the positions from the exchange
        return false;
    }

    /**
     * Send trading data to the web interface.
     *
     * @param currencyPair the currency pair
     * @param barSeries the bar series
     * @param rsiValue RSI value
     * @param macdValue MACD value
     * @param bbUpperValue Bollinger Band upper value
     * @param bbMiddleValue Bollinger Band middle value
     * @param bbLowerValue Bollinger Band lower value
     * @param currentPrice current price
     * @param predictedUp prediction (true if up, false if down)
     */
    private void sendTradingDataToWebInterface(CurrencyPairDTO currencyPair, BarSeries barSeries,
                                              double rsiValue, double macdValue,
                                              double bbUpperValue, double bbMiddleValue, double bbLowerValue,
                                              double currentPrice, boolean predictedUp) {
        TradingData tradingData = new TradingData();

        // Set basic information
        tradingData.setCurrencyPair(currencyPair.toString());
        tradingData.setCurrentPrice(BigDecimal.valueOf(currentPrice));
        tradingData.setTimestamp(ZonedDateTime.now());

        // Set indicator values
        tradingData.setRsiValue(rsiValue);
        tradingData.setMacdValue(macdValue);
        tradingData.setBollingerUpper(bbUpperValue);
        tradingData.setBollingerMiddle(bbMiddleValue);
        tradingData.setBollingerLower(bbLowerValue);

        // Set prediction
        tradingData.setPredictedUp(predictedUp);

        // Set price history for chart
        List<TradingData.PricePoint> priceHistory = new ArrayList<>();
        int historySize = Math.min(barSeries.getBarCount(), 30); // Last 30 bars
        for (int i = barSeries.getEndIndex() - historySize + 1; i <= barSeries.getEndIndex(); i++) {
            if (i >= 0) {
                Bar bar = barSeries.getBar(i);
                priceHistory.add(new TradingData.PricePoint(
                        bar.getEndTime(),
                        BigDecimal.valueOf(bar.getClosePrice().doubleValue())
                ));
            }
        }
        tradingData.setPriceHistory(priceHistory);

        // Set open positions (empty for now)
        tradingData.setOpenPositions(new ArrayList<>());

        // Send to web interface
        tradingController.updateTradingData(tradingData);
    }

    /**
     * Class to record price predictions for self-learning.
     */
    private static class PredictionRecord {
        private final ZonedDateTime timestamp;
        private final double price;
        private final boolean predictedUp;
        private boolean evaluated;

        public PredictionRecord(ZonedDateTime timestamp, double price, boolean predictedUp) {
            this.timestamp = timestamp;
            this.price = price;
            this.predictedUp = predictedUp;
            this.evaluated = false;
        }

        public ZonedDateTime getTimestamp() {
            return timestamp;
        }

        public double getPrice() {
            return price;
        }

        public boolean isPredictedUp() {
            return predictedUp;
        }

        public boolean isEvaluated() {
            return evaluated;
        }

        public void setEvaluated(boolean evaluated) {
            this.evaluated = evaluated;
        }
    }

    /**
     * Class to store trend learning parameters for self-learning.
     */
    private static class TrendLearningParameters {
        // Weights for different indicators in uptrend prediction
        private double rsiUptrendWeight = 1.0;
        private double macdUptrendWeight = 1.0;
        private double bollingerUptrendWeight = 1.0;

        // Weights for different indicators in downtrend prediction
        private double rsiDowntrendWeight = 1.0;
        private double macdDowntrendWeight = 1.0;
        private double bollingerDowntrendWeight = 1.0;

        // Thresholds for indicators
        private double rsiOversoldThreshold = 30.0;
        private double rsiOverboughtThreshold = 70.0;

        // Success counters for learning
        private int uptrendSuccessCount = 0;
        private int uptrendFailureCount = 0;
        private int downtrendSuccessCount = 0;
        private int downtrendFailureCount = 0;

        // Adjust weights based on prediction success/failure
        public void adjustWeights(boolean predictedUp, boolean wasCorrect) {
            double adjustmentFactor = 0.05; // Small adjustment to avoid overreacting

            if (predictedUp) {
                if (wasCorrect) {
                    uptrendSuccessCount++;
                    // Increase weights that worked well
                    rsiUptrendWeight += adjustmentFactor;
                    macdUptrendWeight += adjustmentFactor;
                    bollingerUptrendWeight += adjustmentFactor;
                } else {
                    uptrendFailureCount++;
                    // Decrease weights that didn't work well
                    rsiUptrendWeight -= adjustmentFactor;
                    macdUptrendWeight -= adjustmentFactor;
                    bollingerUptrendWeight -= adjustmentFactor;
                }
            } else {
                if (wasCorrect) {
                    downtrendSuccessCount++;
                    // Increase weights that worked well
                    rsiDowntrendWeight += adjustmentFactor;
                    macdDowntrendWeight += adjustmentFactor;
                    bollingerDowntrendWeight += adjustmentFactor;
                } else {
                    downtrendFailureCount++;
                    // Decrease weights that didn't work well
                    rsiDowntrendWeight -= adjustmentFactor;
                    macdDowntrendWeight -= adjustmentFactor;
                    bollingerDowntrendWeight -= adjustmentFactor;
                }
            }

            // Ensure weights don't go below a minimum value
            rsiUptrendWeight = Math.max(0.1, rsiUptrendWeight);
            macdUptrendWeight = Math.max(0.1, macdUptrendWeight);
            bollingerUptrendWeight = Math.max(0.1, bollingerUptrendWeight);
            rsiDowntrendWeight = Math.max(0.1, rsiDowntrendWeight);
            macdDowntrendWeight = Math.max(0.1, macdDowntrendWeight);
            bollingerDowntrendWeight = Math.max(0.1, bollingerDowntrendWeight);

            // Adjust thresholds based on success/failure ratio
            if (uptrendSuccessCount + uptrendFailureCount > 10) {
                double uptrendSuccessRate = (double) uptrendSuccessCount / (uptrendSuccessCount + uptrendFailureCount);
                if (uptrendSuccessRate < 0.4) {
                    // If uptrend predictions are often wrong, adjust RSI thresholds
                    rsiOversoldThreshold = Math.max(20.0, rsiOversoldThreshold - 1.0);
                } else if (uptrendSuccessRate > 0.7) {
                    // If uptrend predictions are often right, maintain or slightly adjust thresholds
                    rsiOversoldThreshold = Math.min(35.0, rsiOversoldThreshold + 0.5);
                }
            }

            if (downtrendSuccessCount + downtrendFailureCount > 10) {
                double downtrendSuccessRate = (double) downtrendSuccessCount / (downtrendSuccessCount + downtrendFailureCount);
                if (downtrendSuccessRate < 0.4) {
                    // If downtrend predictions are often wrong, adjust RSI thresholds
                    rsiOverboughtThreshold = Math.min(80.0, rsiOverboughtThreshold + 1.0);
                } else if (downtrendSuccessRate > 0.7) {
                    // If downtrend predictions are often right, maintain or slightly adjust thresholds
                    rsiOverboughtThreshold = Math.max(65.0, rsiOverboughtThreshold - 0.5);
                }
            }
        }

        // Getters for all parameters
        public double getRsiOversoldThreshold() {
            return rsiOversoldThreshold;
        }

        public double getRsiOverboughtThreshold() {
            return rsiOverboughtThreshold;
        }

        public double getRsiUptrendWeight() {
            return rsiUptrendWeight;
        }

        public double getMacdUptrendWeight() {
            return macdUptrendWeight;
        }

        public double getBollingerUptrendWeight() {
            return bollingerUptrendWeight;
        }

        public double getRsiDowntrendWeight() {
            return rsiDowntrendWeight;
        }

        public double getMacdDowntrendWeight() {
            return macdDowntrendWeight;
        }

        public double getBollingerDowntrendWeight() {
            return bollingerDowntrendWeight;
        }
    }
}
