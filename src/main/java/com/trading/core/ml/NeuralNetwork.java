package com.trading.core.ml;

import com.trading.config.TradingBotConfig;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;

/**
 * Neural Network implementation for cryptocurrency price prediction
 * Uses enhanced learning formula with ROI-based reward coefficient
 */
@Component
public class NeuralNetwork {
    
    private static final Logger logger = LoggerFactory.getLogger(NeuralNetwork.class);
    
    @Autowired
    private TradingBotConfig config;
    
    private MultiLayerNetwork network;
    private boolean isInitialized = false;
    
    // Enhanced learning formula constants
    private static final double ALPHA = 0.001; // Base learning rate
    private static final double BETA = 0.01;   // Time decay coefficient
    
    @PostConstruct
    public void initialize() {
        try {
            buildNetwork();
            isInitialized = true;
            logger.info("Neural network initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize neural network", e);
        }
    }
    
    /**
     * Build the neural network architecture
     */
    private void buildNetwork() {
        TradingBotConfig.NeuralNetworkConfig nnConfig = config.getNeuralNetwork();
        
        NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                .seed(123)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(nnConfig.getLearningRate()))
                .weightInit(WeightInit.XAVIER)
                .l2(1e-4);
        
        NeuralNetConfiguration.ListBuilder listBuilder = builder.list();
        
        // Input layer
        listBuilder.layer(0, new DenseLayer.Builder()
                .nIn(nnConfig.getInputFeatures())
                .nOut(nnConfig.getHiddenLayers()[0])
                .activation(Activation.RELU)
                .build());
        
        // Hidden layers
        for (int i = 1; i < nnConfig.getHiddenLayers().length; i++) {
            listBuilder.layer(i, new DenseLayer.Builder()
                    .nIn(nnConfig.getHiddenLayers()[i-1])
                    .nOut(nnConfig.getHiddenLayers()[i])
                    .activation(Activation.RELU)
                    .build());
        }
        
        // Output layer (3 outputs: price direction, confidence, position size)
        int lastHiddenLayer = nnConfig.getHiddenLayers().length - 1;
        listBuilder.layer(lastHiddenLayer + 1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(nnConfig.getHiddenLayers()[lastHiddenLayer])
                .nOut(3)
                .activation(Activation.SIGMOID)
                .build());
        
        MultiLayerConfiguration conf = listBuilder.build();
        network = new MultiLayerNetwork(conf);
        network.init();
    }
    
    /**
     * Make prediction based on input features
     */
    public TradingPrediction predict(double[] inputFeatures) {
        if (!isInitialized) {
            throw new IllegalStateException("Neural network not initialized");
        }
        
        INDArray input = Nd4j.create(inputFeatures).reshape(1, inputFeatures.length);
        INDArray output = network.output(input);
        
        // Extract prediction components
        double priceDirection = output.getDouble(0, 0); // 0-1 (0=down, 1=up)
        double confidence = output.getDouble(0, 1);     // 0-1 confidence level
        double positionSize = output.getDouble(0, 2);   // 0-1 recommended position size
        
        return new TradingPrediction(priceDirection, confidence, positionSize);
    }
    
    /**
     * Train the network with trading results using enhanced learning formula
     */
    public void trainWithResult(TradingResult result) {
        if (!isInitialized) {
            logger.warn("Cannot train: Neural network not initialized");
            return;
        }
        
        try {
            // Calculate enhanced reward coefficient
            double rewardCoefficient = calculateRewardCoefficient(result);
            
            // Adjust learning rate based on reward coefficient
            double adjustedLearningRate = config.getNeuralNetwork().getLearningRate() * rewardCoefficient;
            
            // Update network configuration with new learning rate
            network.getUpdater().setLrAndSchedule(adjustedLearningRate, 0);
            
            // Create training data
            INDArray input = Nd4j.create(result.getInputFeatures()).reshape(1, result.getInputFeatures().length);
            INDArray target = createTargetOutput(result);
            
            DataSet dataSet = new DataSet(input, target);
            
            // Train the network
            network.fit(dataSet);
            
            logger.debug("Network trained with reward coefficient: {}, adjusted LR: {}", 
                        rewardCoefficient, adjustedLearningRate);
            
        } catch (Exception e) {
            logger.error("Failed to train neural network", e);
        }
    }
    
    /**
     * Enhanced learning coefficient calculation formula:
     * LearningRate = α * (1 + ROI) * AccuracyFactor * TimeDecay
     */
    private double calculateRewardCoefficient(TradingResult result) {
        // ROI calculation
        double roi = (result.getCurrentBalance() - result.getInitialBalance()) / result.getInitialBalance();
        
        // Accuracy factor
        double accuracyFactor = result.getTotalPredictions() > 0 ? 
            (double) result.getCorrectPredictions() / result.getTotalPredictions() : 0.5;
        
        // Time decay factor
        double timeDecay = 1.0 / (1.0 + BETA * result.getTimeSinceLastTrade());
        
        // Profit multiplier (bounded between 0.5 and 2.0)
        double profitMultiplier = Math.max(0.5, Math.min(2.0, 1.0 + roi));
        
        return ALPHA * profitMultiplier * accuracyFactor * timeDecay;
    }
    
    /**
     * Create target output based on trading result
     */
    private INDArray createTargetOutput(TradingResult result) {
        double[] target = new double[3];
        
        // Price direction target (1.0 if profitable, 0.0 if loss)
        target[0] = result.getProfitLoss() > 0 ? 1.0 : 0.0;
        
        // Confidence target (higher for successful trades)
        target[1] = result.isSuccessful() ? 0.8 : 0.2;
        
        // Position size target (smaller for losses, larger for profits)
        target[2] = result.getProfitLoss() > 0 ? 0.7 : 0.3;
        
        return Nd4j.create(target).reshape(1, 3);
    }
    
    /**
     * Batch training with multiple results
     */
    public void batchTrain(List<TradingResult> results) {
        if (!isInitialized || results.isEmpty()) {
            return;
        }
        
        try {
            for (TradingResult result : results) {
                trainWithResult(result);
            }
            logger.info("Completed batch training with {} results", results.size());
        } catch (Exception e) {
            logger.error("Failed to perform batch training", e);
        }
    }
    
    /**
     * Save the trained model
     */
    public void saveModel(String filePath) {
        if (!isInitialized) {
            throw new IllegalStateException("Cannot save: Neural network not initialized");
        }
        
        try {
            network.save(new java.io.File(filePath));
            logger.info("Neural network model saved to: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to save neural network model", e);
        }
    }
    
    /**
     * Load a pre-trained model
     */
    public void loadModel(String filePath) {
        try {
            network = MultiLayerNetwork.load(new java.io.File(filePath), true);
            isInitialized = true;
            logger.info("Neural network model loaded from: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to load neural network model", e);
        }
    }
    
    /**
     * Get network performance metrics
     */
    public NetworkMetrics getMetrics() {
        if (!isInitialized) {
            return new NetworkMetrics(0, 0, 0);
        }
        
        // These would be calculated based on recent predictions vs actual results
        // For now, return placeholder values
        return new NetworkMetrics(0.75, 0.001, network.numParams());
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Trading prediction result
     */
    public static class TradingPrediction {
        private final double priceDirection;  // 0-1 (0=down, 1=up)
        private final double confidence;      // 0-1 confidence level
        private final double positionSize;    // 0-1 recommended position size
        
        public TradingPrediction(double priceDirection, double confidence, double positionSize) {
            this.priceDirection = priceDirection;
            this.confidence = confidence;
            this.positionSize = positionSize;
        }
        
        public boolean isPredictingUp() {
            return priceDirection > 0.5;
        }
        
        public boolean isPredictingDown() {
            return priceDirection < 0.5;
        }
        
        public boolean isHighConfidence() {
            return confidence > 0.7;
        }
        
        // Getters
        public double getPriceDirection() { return priceDirection; }
        public double getConfidence() { return confidence; }
        public double getPositionSize() { return positionSize; }
    }
    
    /**
     * Network performance metrics
     */
    public static class NetworkMetrics {
        private final double accuracy;
        private final double loss;
        private final long parameters;
        
        public NetworkMetrics(double accuracy, double loss, long parameters) {
            this.accuracy = accuracy;
            this.loss = loss;
            this.parameters = parameters;
        }
        
        // Getters
        public double getAccuracy() { return accuracy; }
        public double getLoss() { return loss; }
        public long getParameters() { return parameters; }
    }
}