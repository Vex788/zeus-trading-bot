package com.trading.domain.entities;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trading history entity for tracking all trading operations
 */
@Entity
@Table(name = "trading_history")
public class TradingHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "trade_time")
    private LocalDateTime tradeTime;
    
    @Column(name = "trading_pair", length = 20)
    private String tradingPair;
    
    @Column(name = "action", length = 10)
    @Enumerated(EnumType.STRING)
    private TradeAction action;
    
    @Column(name = "amount", precision = 20, scale = 8)
    private BigDecimal amount;
    
    @Column(name = "price", precision = 20, scale = 8)
    private BigDecimal price;
    
    @Column(name = "profit_loss", precision = 20, scale = 8)
    private BigDecimal profitLoss;
    
    @Column(name = "balance_after", precision = 20, scale = 8)
    private BigDecimal balanceAfter;
    
    @Column(name = "is_virtual")
    private Boolean isVirtual = false;
    
    @Column(name = "strategy_used", length = 50)
    private String strategyUsed;
    
    @Column(name = "ml_confidence", precision = 5, scale = 4)
    private BigDecimal mlConfidence;
    
    @PrePersist
    protected void onCreate() {
        if (tradeTime == null) {
            tradeTime = LocalDateTime.now();
        }
    }
    
    // Constructors
    public TradingHistory() {}
    
    public TradingHistory(String tradingPair, TradeAction action, BigDecimal amount, 
                         BigDecimal price, BigDecimal balanceAfter, Boolean isVirtual) {
        this.tradingPair = tradingPair;
        this.action = action;
        this.amount = amount;
        this.price = price;
        this.balanceAfter = balanceAfter;
        this.isVirtual = isVirtual;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getTradeTime() { return tradeTime; }
    public void setTradeTime(LocalDateTime tradeTime) { this.tradeTime = tradeTime; }
    
    public String getTradingPair() { return tradingPair; }
    public void setTradingPair(String tradingPair) { this.tradingPair = tradingPair; }
    
    public TradeAction getAction() { return action; }
    public void setAction(TradeAction action) { this.action = action; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public BigDecimal getProfitLoss() { return profitLoss; }
    public void setProfitLoss(BigDecimal profitLoss) { this.profitLoss = profitLoss; }
    
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    
    public Boolean getIsVirtual() { return isVirtual; }
    public void setIsVirtual(Boolean isVirtual) { this.isVirtual = isVirtual; }
    
    public String getStrategyUsed() { return strategyUsed; }
    public void setStrategyUsed(String strategyUsed) { this.strategyUsed = strategyUsed; }
    
    public BigDecimal getMlConfidence() { return mlConfidence; }
    public void setMlConfidence(BigDecimal mlConfidence) { this.mlConfidence = mlConfidence; }
    
    public enum TradeAction {
        BUY, SELL
    }
}