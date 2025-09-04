package com.trading;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the main application
 */
@SpringBootTest
@ActiveProfiles("test")
class CryptoTradingBotApplicationTests {

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
        // It will fail if there are any configuration issues or missing dependencies
    }

}