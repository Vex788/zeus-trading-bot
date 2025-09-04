package com.trading.core.ml;

import com.trading.config.TradingBotConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NeuralNetwork
 */
@ExtendWith(MockitoExtension.class)
class NeuralNetworkTest {
    
    @Mock
    private TradingBotConfig config;
    
    @Mock
    private TradingBotConfig.NeuralNetworkConfig nnConfig;
    
    @InjectMocks
    private NeuralNetwork neuralNetwork;
    
    @BeforeEach
    void setUp() {
        when(config.getNeuralNetwork()).thenReturn(nnConfig);
        when(nnConfig.getLearningRate()).thenReturn(0.001);
        when(nnConfig.getEpochs()).thenReturn(100);
        when(nnConfig.getBatchSize()).thenReturn(32);
        when(nnConfig.getHiddenLayers()).thenReturn(new int[]{64, 32, 16});
        when(nnConfig.getInputFeatures()).thenReturn(20);
    }
    
    @Test
    void testNeuralNetworkInitialization() {
        // When
        neuralNetwork.initialize();
        
        // Then
        assertTrue(neuralNetwork.isInitialized());
    }
    
    @Test
    void testPredictionWithValidInput() {
        // Given
        neuralNetwork.initialize();
        double[] inputFeatures = new double[20];
        for (int i = 0; i < inputFeatures.length; i++) {
            inputFeatures[i] = Math.random();
        }
        
        // When
        NeuralNetwork.TradingPrediction prediction = neuralNetwork.predict(inputFeatures);
        
        // Then
        assertNotNull(prediction);
        assertTrue(prediction.getPriceDirection() >= 0 && prediction.getPriceDirection() <= 1);
        assertTrue(prediction.getConfidence() >= 0 && prediction.getConfidence() <= 1);
        assertTrue(prediction.getPositionSize() >= 0 && prediction.getPositionSize() <= 1);
    }
    
    @Test
    void testPredictionWithoutInitialization() {
        // Given
        double[] inputFeatures = new double[20];
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            neuralNetwork.predict(inputFeatures);
        });
    }
    
    @Test
    void testTrainingWithResult() {
        // Given
        neuralNetwork.initialize();
        double[] inputFeatures = new double[20];
        TradingResult result = new TradingResult(
            inputFeatures, 105.0, 100.0, 8, 10, 2.0, 5.0, true
        );
        
        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> {
            neuralNetwork.trainWithResult(result);
        });
    }
    
    @Test
    void testBatchTraining() {
        // Given
        neuralNetwork.initialize();
        double[] inputFeatures = new double[20];
        
        java.util.List<TradingResult> results = java.util.List.of(
            new TradingResult(inputFeatures, 105.0, 100.0, 8, 10, 2.0, 5.0, true),
            new TradingResult(inputFeatures, 103.0, 100.0, 7, 10, 1.5, 3.0, true),
            new TradingResult(inputFeatures, 98.0, 100.0, 5, 10, 3.0, -2.0, false)
        );
        
        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> {
            neuralNetwork.batchTrain(results);
        });
    }
    
    @Test
    void testGetMetrics() {
        // Given
        neuralNetwork.initialize();
        
        // When
        NeuralNetwork.NetworkMetrics metrics = neuralNetwork.getMetrics();
        
        // Then
        assertNotNull(metrics);
        assertTrue(metrics.getParameters() > 0);
        assertTrue(metrics.getAccuracy() >= 0);
        assertTrue(metrics.getLoss() >= 0);
    }
    
    @Test
    void testPredictionDirectionMethods() {
        // Given
        NeuralNetwork.TradingPrediction upPrediction = 
            new NeuralNetwork.TradingPrediction(0.8, 0.7, 0.5);
        NeuralNetwork.TradingPrediction downPrediction = 
            new NeuralNetwork.TradingPrediction(0.3, 0.6, 0.4);
        
        // Then
        assertTrue(upPrediction.isPredictingUp());
        assertFalse(upPrediction.isPredictingDown());
        
        assertFalse(downPrediction.isPredictingUp());
        assertTrue(downPrediction.isPredictingDown());
    }
    
    @Test
    void testHighConfidencePrediction() {
        // Given
        NeuralNetwork.TradingPrediction highConfidence = 
            new NeuralNetwork.TradingPrediction(0.8, 0.85, 0.6);
        NeuralNetwork.TradingPrediction lowConfidence = 
            new NeuralNetwork.TradingPrediction(0.6, 0.5, 0.4);
        
        // Then
        assertTrue(highConfidence.isHighConfidence());
        assertFalse(lowConfidence.isHighConfidence());
    }
}