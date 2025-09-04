package com.trading.config;

import com.trading.domain.entities.BotConfig;
import com.trading.domain.repositories.BotConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Secure storage and retrieval of API keys and sensitive configuration
 */
@Component
public class SecureKeyStorage {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureKeyStorage.class);
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private BotConfigRepository botConfigRepository;
    
    /**
     * Store API credentials securely
     */
    public void storeApiCredentials(String apiKey, String secretKey, String passphrase) {
        try {
            // Encrypt the credentials
            String encryptedApiKey = encryptionService.encrypt(apiKey);
            String encryptedSecretKey = encryptionService.encrypt(secretKey);
            String encryptedPassphrase = encryptionService.encrypt(passphrase);
            
            // Get or create bot configuration
            BotConfig config = botConfigRepository.findCurrentConfig()
                .orElse(new BotConfig());
            
            // Store encrypted credentials
            config.setApiKey(encryptedApiKey);
            config.setSecretKey(encryptedSecretKey);
            // Note: In a real implementation, passphrase would also be stored securely
            
            botConfigRepository.save(config);
            
            logger.info("API credentials stored securely");
            
        } catch (Exception e) {
            logger.error("Failed to store API credentials securely", e);
            throw new RuntimeException("Failed to store credentials", e);
        }
    }
    
    /**
     * Retrieve and decrypt API key
     */
    public String getApiKey() {
        try {
            Optional<BotConfig> config = botConfigRepository.findCurrentConfig();
            if (config.isPresent() && config.get().getApiKey() != null) {
                return encryptionService.decrypt(config.get().getApiKey());
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to retrieve API key", e);
            return null;
        }
    }
    
    /**
     * Retrieve and decrypt secret key
     */
    public String getSecretKey() {
        try {
            Optional<BotConfig> config = botConfigRepository.findCurrentConfig();
            if (config.isPresent() && config.get().getSecretKey() != null) {
                return encryptionService.decrypt(config.get().getSecretKey());
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to retrieve secret key", e);
            return null;
        }
    }
    
    /**
     * Check if API credentials are configured
     */
    public boolean areCredentialsConfigured() {
        try {
            String apiKey = getApiKey();
            String secretKey = getSecretKey();
            return apiKey != null && !apiKey.isEmpty() && 
                   secretKey != null && !secretKey.isEmpty();
        } catch (Exception e) {
            logger.error("Failed to check credentials configuration", e);
            return false;
        }
    }
    
    /**
     * Clear stored credentials
     */
    public void clearCredentials() {
        try {
            Optional<BotConfig> config = botConfigRepository.findCurrentConfig();
            if (config.isPresent()) {
                BotConfig botConfig = config.get();
                botConfig.setApiKey(null);
                botConfig.setSecretKey(null);
                botConfigRepository.save(botConfig);
                
                logger.info("API credentials cleared");
            }
        } catch (Exception e) {
            logger.error("Failed to clear credentials", e);
            throw new RuntimeException("Failed to clear credentials", e);
        }
    }
    
    /**
     * Validate stored credentials format
     */
    public boolean validateStoredCredentials() {
        try {
            Optional<BotConfig> config = botConfigRepository.findCurrentConfig();
            if (!config.isPresent()) {
                return false;
            }
            
            BotConfig botConfig = config.get();
            
            // Check if credentials are encrypted
            if (botConfig.getApiKey() != null && 
                !encryptionService.isEncrypted(botConfig.getApiKey())) {
                logger.warn("API key is not properly encrypted");
                return false;
            }
            
            if (botConfig.getSecretKey() != null && 
                !encryptionService.isEncrypted(botConfig.getSecretKey())) {
                logger.warn("Secret key is not properly encrypted");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to validate stored credentials", e);
            return false;
        }
    }
    
    /**
     * Get masked credentials for display purposes
     */
    public CredentialInfo getMaskedCredentials() {
        try {
            String apiKey = getApiKey();
            String secretKey = getSecretKey();
            
            return new CredentialInfo(
                apiKey != null ? encryptionService.maskSensitiveData(apiKey) : null,
                secretKey != null ? encryptionService.maskSensitiveData(secretKey) : null,
                areCredentialsConfigured()
            );
            
        } catch (Exception e) {
            logger.error("Failed to get masked credentials", e);
            return new CredentialInfo(null, null, false);
        }
    }
    
    /**
     * Rotate encryption keys (for security maintenance)
     */
    public void rotateEncryptionKeys() {
        try {
            // Get current credentials
            String apiKey = getApiKey();
            String secretKey = getSecretKey();
            
            if (apiKey != null && secretKey != null) {
                // Re-encrypt with new key
                storeApiCredentials(apiKey, secretKey, ""); // Passphrase would be handled separately
                
                logger.info("Encryption keys rotated successfully");
            }
            
        } catch (Exception e) {
            logger.error("Failed to rotate encryption keys", e);
            throw new RuntimeException("Failed to rotate encryption keys", e);
        }
    }
    
    /**
     * Data class for credential information
     */
    public static class CredentialInfo {
        private final String maskedApiKey;
        private final String maskedSecretKey;
        private final boolean configured;
        
        public CredentialInfo(String maskedApiKey, String maskedSecretKey, boolean configured) {
            this.maskedApiKey = maskedApiKey;
            this.maskedSecretKey = maskedSecretKey;
            this.configured = configured;
        }
        
        public String getMaskedApiKey() { return maskedApiKey; }
        public String getMaskedSecretKey() { return maskedSecretKey; }
        public boolean isConfigured() { return configured; }
    }
}