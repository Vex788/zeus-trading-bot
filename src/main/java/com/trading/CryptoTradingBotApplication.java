package com.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for the Cryptocurrency Auto-Trading Bot
 * 
 * Features:
 * - AI-powered trading decisions using neural networks
 * - KuCoin API integration for real trading
 * - Shadow mode for risk-free strategy testing
 * - Real-time web dashboard
 * - Comprehensive risk management
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
public class CryptoTradingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoTradingBotApplication.class, args);
    }
}