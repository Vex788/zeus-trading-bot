package com.trading.domain.entities;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Portfolio entity for tracking current holdings and balance
 */
@Entity
@Table(name = "portfolio")
public class Portfolio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "currency", length = 10)
    private String currency;
    
    @Column(name = "balance", precision = 20, scale = 8)
    private BigDecimal balance;
    
    @Column(name = "available_balance", precision = 20, scale = 8)
    private BigDecimal availableBalance;
    
    @Column(name = "locked_balance", precision = 20, scale = 8)
    private BigDecimal lockedBalance = BigDecimal.ZERO;
    
    @Column(name = "is_virtual")
    private Boolean isVirtual = false;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
    
    // Constructors
    public Portfolio() {}
    
    public Portfolio(String currency, BigDecimal balance, Boolean isVirtual) {
        this.currency = currency;
        this.balance = balance;
        this.availableBalance = balance;
        this.isVirtual = isVirtual;
    }
    
    // Business methods
    public void updateBalance(BigDecimal newBalance) {
        this.balance = newBalance;
        this.availableBalance = newBalance.subtract(lockedBalance);
    }
    
    public void lockAmount(BigDecimal amount) {
        if (availableBalance.compareTo(amount) >= 0) {
            this.lockedBalance = lockedBalance.add(amount);
            this.availableBalance = availableBalance.subtract(amount);
        } else {
            throw new IllegalArgumentException("Insufficient available balance to lock");
        }
    }
    
    public void unlockAmount(BigDecimal amount) {
        if (lockedBalance.compareTo(amount) >= 0) {
            this.lockedBalance = lockedBalance.subtract(amount);
            this.availableBalance = availableBalance.add(amount);
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    
    public BigDecimal getLockedBalance() { return lockedBalance; }
    public void setLockedBalance(BigDecimal lockedBalance) { this.lockedBalance = lockedBalance; }
    
    public Boolean getIsVirtual() { return isVirtual; }
    public void setIsVirtual(Boolean isVirtual) { this.isVirtual = isVirtual; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}