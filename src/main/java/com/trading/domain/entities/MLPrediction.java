package com.trading.domain.entities;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Machine Learning prediction entity for tracking neural network performance
 */
@Entity
@Table(name = "ml_predictions")
public class MLPrediction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "prediction_time")
    private LocalDateTime predictionTime;
    
    @Column(name = "trading_pair", length = 20)
    private String tradingPair;
    
    @Column(name = "predicted_price", precision = 20, scale = 8)
    private BigDecimal predictedPrice;
    
    @Column(name = "actual_price", precision = 20, scale = 8)
    private BigDecimal actualPrice;
    
    @Column(name = "accuracy_score", precision = 5, scale = 4)
    private BigDecimal accuracyScore;
    
    @Column(name = "reward_coefficient", precision = 10, scale = 8)
    private BigDecimal rewardCoefficient;
    
    @Column(name = "prediction_direction")
    @Enumerated(EnumType.STRING)
    private PredictionDirection direction;
    
    @Column(name = "confidence_level", precision = 5, scale = 4)
    private BigDecimal confidenceLevel;
    
    @PrePersist
    protected void onCreate() {
        if (predictionTime == null) {
            predictionTime = LocalDateTime.now();
        }
    }
    
    // Constructors
    public MLPrediction() {}
    
    public MLPrediction(String tradingPair, BigDecimal predictedPrice, 
                       PredictionDirection direction, BigDecimal confidenceLevel) {
        this.tradingPair = tradingPair;
        this.predictedPrice = predictedPrice;
        this.direction = direction;
        this.confidenceLevel = confidenceLevel;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getPredictionTime() { return predictionTime; }
    public void setPredictionTime(LocalDateTime predictionTime) { this.predictionTime = predictionTime; }
    
    public String getTradingPair() { return tradingPair; }
    public void setTradingPair(String tradingPair) { this.tradingPair = tradingPair; }
    
    public BigDecimal getPredictedPrice() { return predictedPrice; }
    public void setPredictedPrice(BigDecimal predictedPrice) { this.predictedPrice = predictedPrice; }
    
    public BigDecimal getActualPrice() { return actualPrice; }
    public void setActualPrice(BigDecimal actualPrice) { this.actualPrice = actualPrice; }
    
    public BigDecimal getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(BigDecimal accuracyScore) { this.accuracyScore = accuracyScore; }
    
    public BigDecimal getRewardCoefficient() { return rewardCoefficient; }
    public void setRewardCoefficient(BigDecimal rewardCoefficient) { this.rewardCoefficient = rewardCoefficient; }
    
    public PredictionDirection getDirection() { return direction; }
    public void setDirection(PredictionDirection direction) { this.direction = direction; }
    
    public BigDecimal getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(BigDecimal confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    
    public enum PredictionDirection {
        UP, DOWN, NEUTRAL
    }
}