package com.trading.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EncryptionService
 */
class EncryptionServiceTest {
    
    private EncryptionService encryptionService;
    
    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        // Set a test encryption key
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", "testEncryptionKey123456789012");
    }
    
    @Test
    void testEncryptAndDecrypt() {
        // Given
        String plainText = "mySecretApiKey123";
        
        // When
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);
        
        // Then
        assertNotEquals(plainText, encrypted);
        assertEquals(plainText, decrypted);
    }
    
    @Test
    void testEncryptNullValue() {
        // When
        String encrypted = encryptionService.encrypt(null);
        
        // Then
        assertNull(encrypted);
    }
    
    @Test
    void testEncryptEmptyString() {
        // When
        String encrypted = encryptionService.encrypt("");
        
        // Then
        assertEquals("", encrypted);
    }
    
    @Test
    void testDecryptNullValue() {
        // When
        String decrypted = encryptionService.decrypt(null);
        
        // Then
        assertNull(decrypted);
    }
    
    @Test
    void testDecryptEmptyString() {
        // When
        String decrypted = encryptionService.decrypt("");
        
        // Then
        assertEquals("", decrypted);
    }
    
    @Test
    void testIsEncryptedWithValidBase64() {
        // Given
        String validBase64 = "dGVzdA=="; // "test" in base64
        
        // When
        boolean isEncrypted = encryptionService.isEncrypted(validBase64);
        
        // Then
        assertTrue(isEncrypted);
    }
    
    @Test
    void testIsEncryptedWithInvalidBase64() {
        // Given
        String invalidBase64 = "not-base64!@#";
        
        // When
        boolean isEncrypted = encryptionService.isEncrypted(invalidBase64);
        
        // Then
        assertFalse(isEncrypted);
    }
    
    @Test
    void testIsEncryptedWithNullValue() {
        // When
        boolean isEncrypted = encryptionService.isEncrypted(null);
        
        // Then
        assertFalse(isEncrypted);
    }
    
    @Test
    void testIsEncryptedWithEmptyString() {
        // When
        boolean isEncrypted = encryptionService.isEncrypted("");
        
        // Then
        assertFalse(isEncrypted);
    }
    
    @Test
    void testMaskSensitiveData() {
        // Given
        String sensitiveData = "mySecretApiKey123456";
        
        // When
        String masked = encryptionService.maskSensitiveData(sensitiveData);
        
        // Then
        assertTrue(masked.startsWith("mySe"));
        assertTrue(masked.contains("*"));
        assertNotEquals(sensitiveData, masked);
    }
    
    @Test
    void testMaskSensitiveDataShortString() {
        // Given
        String shortData = "abc";
        
        // When
        String masked = encryptionService.maskSensitiveData(shortData);
        
        // Then
        assertEquals("****", masked);
    }
    
    @Test
    void testMaskSensitiveDataNullValue() {
        // When
        String masked = encryptionService.maskSensitiveData(null);
        
        // Then
        assertEquals("****", masked);
    }
    
    @Test
    void testGenerateSecureKey() {
        // When
        String key1 = encryptionService.generateSecureKey();
        String key2 = encryptionService.generateSecureKey();
        
        // Then
        assertNotNull(key1);
        assertNotNull(key2);
        assertNotEquals(key1, key2); // Should generate different keys
        assertTrue(encryptionService.isEncrypted(key1)); // Should be valid base64
        assertTrue(encryptionService.isEncrypted(key2)); // Should be valid base64
    }
    
    @Test
    void testHashPassword() {
        // Given
        String password = "myPassword123";
        String salt = "randomSalt";
        
        // When
        String hash1 = encryptionService.hashPassword(password, salt);
        String hash2 = encryptionService.hashPassword(password, salt);
        
        // Then
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertEquals(hash1, hash2); // Same password and salt should produce same hash
        assertNotEquals(password, hash1); // Hash should be different from original password
    }
    
    @Test
    void testGenerateSalt() {
        // When
        String salt1 = encryptionService.generateSalt();
        String salt2 = encryptionService.generateSalt();
        
        // Then
        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals(salt1, salt2); // Should generate different salts
        assertTrue(encryptionService.isEncrypted(salt1)); // Should be valid base64
        assertTrue(encryptionService.isEncrypted(salt2)); // Should be valid base64
    }
    
    @Test
    void testSecureEquals() {
        // Given
        String str1 = "testString";
        String str2 = "testString";
        String str3 = "differentString";
        
        // When & Then
        assertTrue(encryptionService.secureEquals(str1, str2));
        assertFalse(encryptionService.secureEquals(str1, str3));
        assertFalse(encryptionService.secureEquals(str1, null));
        assertFalse(encryptionService.secureEquals(null, str2));
        assertTrue(encryptionService.secureEquals(null, null));
    }
    
    @Test
    void testClearSensitiveData() {
        // Given
        char[] sensitiveData = "password123".toCharArray();
        char[] originalData = sensitiveData.clone();
        
        // When
        encryptionService.clearSensitiveData(sensitiveData);
        
        // Then
        for (char c : sensitiveData) {
            assertEquals('\0', c);
        }
        // Verify original data was actually different
        assertFalse(java.util.Arrays.equals(originalData, sensitiveData));
    }
    
    @Test
    void testClearSensitiveDataWithNull() {
        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> {
            encryptionService.clearSensitiveData(null);
        });
    }
    
    @Test
    void testEncryptionConsistency() {
        // Given
        String plainText = "consistencyTest";
        
        // When
        String encrypted1 = encryptionService.encrypt(plainText);
        String encrypted2 = encryptionService.encrypt(plainText);
        
        // Then
        // Note: With ECB mode, same input produces same output
        // In production, you might want to use CBC or GCM mode with IV for better security
        assertEquals(encrypted1, encrypted2);
        
        String decrypted1 = encryptionService.decrypt(encrypted1);
        String decrypted2 = encryptionService.decrypt(encrypted2);
        
        assertEquals(plainText, decrypted1);
        assertEquals(plainText, decrypted2);
    }
}