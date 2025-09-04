package com.trading.domain.repositories;

import com.trading.domain.entities.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Portfolio entity
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    
    /**
     * Find portfolio by currency and virtual flag
     */
    Optional<Portfolio> findByCurrencyAndIsVirtual(String currency, Boolean isVirtual);
    
    /**
     * Find all virtual portfolios
     */
    List<Portfolio> findByIsVirtualTrue();
    
    /**
     * Find all real portfolios
     */
    List<Portfolio> findByIsVirtualFalse();
    
    /**
     * Calculate total portfolio value in base currency
     */
    @Query("SELECT SUM(p.balance) FROM Portfolio p WHERE p.isVirtual = :isVirtual")
    BigDecimal calculateTotalValue(@Param("isVirtual") Boolean isVirtual);
    
    /**
     * Find portfolios with non-zero balance
     */
    @Query("SELECT p FROM Portfolio p WHERE p.balance > 0 AND p.isVirtual = :isVirtual ORDER BY p.balance DESC")
    List<Portfolio> findNonZeroBalances(@Param("isVirtual") Boolean isVirtual);
}