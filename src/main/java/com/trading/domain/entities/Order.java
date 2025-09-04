package com.trading.domain.entities;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order entity representing trading orders
 */
@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", unique = true, length = 100)
    private String orderId;
    
    @Column(name = "trading_pair", length = 20)
    private String tradingPair;
    
    @Column(name = "side", length = 10)
    @Enumerated(EnumType.STRING)
    private OrderSide side;
    
    @Column(name = "price", precision = 20, scale = 8)
    private BigDecimal price;
    
    @Column(name = "amount", precision = 20, scale = 8)
    private BigDecimal amount;
    
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Column(name = "is_virtual")
    private Boolean isVirtual = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "filled_at")
    private LocalDateTime filledAt;
    
    @Column(name = "filled_amount", precision = 20, scale = 8)
    private BigDecimal filledAmount = BigDecimal.ZERO;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public Order() {}
    
    public Order(String orderId, String tradingPair, OrderSide side, 
                 BigDecimal price, BigDecimal amount, Boolean isVirtual) {
        this.orderId = orderId;
        this.tradingPair = tradingPair;
        this.side = side;
        this.price = price;
        this.amount = amount;
        this.isVirtual = isVirtual;
        this.status = OrderStatus.PENDING;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getTradingPair() { return tradingPair; }
    public void setTradingPair(String tradingPair) { this.tradingPair = tradingPair; }
    
    public OrderSide getSide() { return side; }
    public void setSide(OrderSide side) { this.side = side; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public Boolean getIsVirtual() { return isVirtual; }
    public void setIsVirtual(Boolean isVirtual) { this.isVirtual = isVirtual; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getFilledAt() { return filledAt; }
    public void setFilledAt(LocalDateTime filledAt) { this.filledAt = filledAt; }
    
    public BigDecimal getFilledAmount() { return filledAmount; }
    public void setFilledAmount(BigDecimal filledAmount) { this.filledAmount = filledAmount; }
    
    public enum OrderSide {
        BUY, SELL
    }
    
    public enum OrderStatus {
        PENDING, FILLED, CANCELLED, PARTIALLY_FILLED, FAILED
    }
}