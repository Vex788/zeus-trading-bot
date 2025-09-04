package com.trading.domain.entities;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bot configuration entity
 */
@Entity
@Table(name = "bot_config")
public class BotConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "mode", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TradingMode mode;
    
    @Column(name = "api_key")
    private String apiKey;
    
    @Column(name = "secret_key")
    private String secretKey;
    
    @Column(name = "virtual_balance", precision = 20, scale = 8)
    private BigDecimal virtualBalance;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public BotConfig() {}
    
    public BotConfig(TradingMode mode, BigDecimal virtualBalance) {
        this.mode = mode;
        this.virtualBalance = virtualBalance;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public TradingMode getMode() { return mode; }
    public void setMode(TradingMode mode) { this.mode = mode; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    
    public BigDecimal getVirtualBalance() { return virtualBalance; }
    public void setVirtualBalance(BigDecimal virtualBalance) { this.virtualBalance = virtualBalance; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public enum TradingMode {
        PRODUCTION, SHADOW
    }
}