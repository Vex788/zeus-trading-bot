package com.trading.domain.repositories;

import com.trading.domain.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find order by external order ID
     */
    Optional<Order> findByOrderId(String orderId);
    
    /**
     * Find orders by trading pair
     */
    List<Order> findByTradingPairOrderByCreatedAtDesc(String tradingPair);
    
    /**
     * Find orders by status
     */
    List<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status);
    
    /**
     * Find virtual orders
     */
    List<Order> findByIsVirtualTrueOrderByCreatedAtDesc();
    
    /**
     * Find orders within date range
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find active orders (pending or partially filled)
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'PARTIALLY_FILLED') ORDER BY o.createdAt DESC")
    List<Order> findActiveOrders();
    
    /**
     * Count orders by status and virtual flag
     */
    long countByStatusAndIsVirtual(Order.OrderStatus status, Boolean isVirtual);
}