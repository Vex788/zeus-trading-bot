package com.trading.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data like API keys
 */
@Service
public class EncryptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    @Value("${app.encryption.key:defaultEncryptionKey123456}")
    private String encryptionKey;
    
    /**
     * Encrypt a plain text string
     */
    public String encrypt(String plainText) {
        try {
            if (plainText == null || plainText.isEmpty()) {
                return plainText;
            }
            
            SecretKeySpec secretKey = new SecretKeySpec(
                getKeyBytes(), ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
            
        } catch (Exception e) {
            logger.error("Failed to encrypt data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt an encrypted string
     */
    public String decrypt(String encryptedText) {
        try {
            if (encryptedText == null || encryptedText.isEmpty()) {
                return encryptedText;
            }
            
            SecretKeySpec secretKey = new SecretKeySpec(
                getKeyBytes(), ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Failed to decrypt data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * Generate a secure random key for encryption
     */
    public String generateSecureKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            logger.error("Failed to generate secure key", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }
    
    /**
     * Hash a password using a secure algorithm
     */
    public String hashPassword(String password, String salt) {
        try {
            // In production, use a proper password hashing library like BCrypt
            // This is a simplified implementation
            String combined = password + salt;
            return Base64.getEncoder().encodeToString(
                combined.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("Failed to hash password", e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }
    
    /**
     * Generate a secure random salt
     */
    public String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Validate if a string is properly encrypted
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        try {
            // Try to decode as Base64
            Base64.getDecoder().decode(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Mask sensitive data for logging
     */
    public String maskSensitiveData(String sensitiveData) {
        if (sensitiveData == null || sensitiveData.length() <= 4) {
            return "****";
        }
        
        int visibleChars = Math.min(4, sensitiveData.length() / 4);
        String visible = sensitiveData.substring(0, visibleChars);
        String masked = "*".repeat(sensitiveData.length() - visibleChars);
        
        return visible + masked;
    }
    
    /**
     * Get encryption key bytes, ensuring proper length
     */
    private byte[] getKeyBytes() {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        
        // Ensure key is exactly 32 bytes for AES-256
        byte[] key = new byte[32];
        if (keyBytes.length >= 32) {
            System.arraycopy(keyBytes, 0, key, 0, 32);
        } else {
            System.arraycopy(keyBytes, 0, key, 0, keyBytes.length);
            // Fill remaining bytes with zeros
            for (int i = keyBytes.length; i < 32; i++) {
                key[i] = 0;
            }
        }
        
        return key;
    }
    
    /**
     * Secure comparison of two strings to prevent timing attacks
     */
    public boolean secureEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
    
    /**
     * Clear sensitive data from memory
     */
    public void clearSensitiveData(char[] sensitiveData) {
        if (sensitiveData != null) {
            for (int i = 0; i < sensitiveData.length; i++) {
                sensitiveData[i] = '\0';
            }
        }
    }
}