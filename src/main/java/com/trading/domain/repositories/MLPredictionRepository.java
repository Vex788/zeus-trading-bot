package com.trading.domain.repositories;

import com.trading.domain.entities.MLPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for MLPrediction entity
 */
@Repository
public interface MLPredictionRepository extends JpaRepository<MLPrediction, Long> {
    
    /**
     * Find predictions by trading pair
     */
    List<MLPrediction> findByTradingPairOrderByPredictionTimeDesc(String tradingPair);
    
    /**
     * Find predictions within date range
     */
    @Query("SELECT mp FROM MLPrediction mp WHERE mp.predictionTime BETWEEN :startDate AND :endDate ORDER BY mp.predictionTime DESC")
    List<MLPrediction> findPredictionsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);
    
    /**
     * Calculate average accuracy for a trading pair
     */
    @Query("SELECT AVG(mp.accuracyScore) FROM MLPrediction mp WHERE mp.tradingPair = :tradingPair AND mp.accuracyScore IS NOT NULL")
    BigDecimal calculateAverageAccuracy(@Param("tradingPair") String tradingPair);
    
    /**
     * Calculate overall accuracy
     */
    @Query("SELECT AVG(mp.accuracyScore) FROM MLPrediction mp WHERE mp.accuracyScore IS NOT NULL")
    BigDecimal calculateOverallAccuracy();
    
    /**
     * Find recent predictions without actual price (for updating)
     */
    @Query("SELECT mp FROM MLPrediction mp WHERE mp.actualPrice IS NULL AND mp.predictionTime > :cutoffTime ORDER BY mp.predictionTime DESC")
    List<MLPrediction> findPendingPredictions(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Count total predictions
     */
    long countByTradingPair(String tradingPair);
    
    /**
     * Count correct predictions (accuracy > threshold)
     */
    @Query("SELECT COUNT(mp) FROM MLPrediction mp WHERE mp.tradingPair = :tradingPair AND mp.accuracyScore > :threshold")
    long countCorrectPredictions(@Param("tradingPair") String tradingPair, @Param("threshold") BigDecimal threshold);
}