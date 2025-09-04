package com.trading.domain.repositories;

import com.trading.domain.entities.TradingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for TradingHistory entity
 */
@Repository
public interface TradingHistoryRepository extends JpaRepository<TradingHistory, Long> {
    
    /**
     * Find trading history by trading pair
     */
    List<TradingHistory> findByTradingPairOrderByTradeTimeDesc(String tradingPair);
    
    /**
     * Find trading history within date range
     */
    @Query("SELECT th FROM TradingHistory th WHERE th.tradeTime BETWEEN :startDate AND :endDate ORDER BY th.tradeTime DESC")
    List<TradingHistory> findTradesBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find virtual trades
     */
    List<TradingHistory> findByIsVirtualTrueOrderByTradeTimeDesc();
    
    /**
     * Calculate total profit/loss
     */
    @Query("SELECT SUM(th.profitLoss) FROM TradingHistory th WHERE th.isVirtual = :isVirtual")
    BigDecimal calculateTotalProfitLoss(@Param("isVirtual") Boolean isVirtual);
    
    /**
     * Calculate profit/loss for a specific trading pair
     */
    @Query("SELECT SUM(th.profitLoss) FROM TradingHistory th WHERE th.tradingPair = :tradingPair AND th.isVirtual = :isVirtual")
    BigDecimal calculateProfitLossForPair(@Param("tradingPair") String tradingPair, @Param("isVirtual") Boolean isVirtual);
    
    /**
     * Calculate daily profit/loss
     */
    @Query("SELECT SUM(th.profitLoss) FROM TradingHistory th WHERE DATE(th.tradeTime) = DATE(:date) AND th.isVirtual = :isVirtual")
    BigDecimal calculateDailyProfitLoss(@Param("date") LocalDateTime date, @Param("isVirtual") Boolean isVirtual);
    
    /**
     * Find recent trades for performance analysis
     */
    @Query("SELECT th FROM TradingHistory th WHERE th.tradeTime > :cutoffTime ORDER BY th.tradeTime DESC")
    List<TradingHistory> findRecentTrades(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Count trades by action
     */
    long countByActionAndIsVirtual(TradingHistory.TradeAction action, Boolean isVirtual);
}