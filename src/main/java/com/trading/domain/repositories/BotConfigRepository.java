package com.trading.domain.repositories;

import com.trading.domain.entities.BotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for BotConfig entity
 */
@Repository
public interface BotConfigRepository extends JpaRepository<BotConfig, Long> {
    
    /**
     * Find the current active configuration
     */
    @Query("SELECT bc FROM BotConfig bc ORDER BY bc.createdAt DESC")
    Optional<BotConfig> findCurrentConfig();
    
    /**
     * Find configuration by mode
     */
    Optional<BotConfig> findByMode(BotConfig.TradingMode mode);
}