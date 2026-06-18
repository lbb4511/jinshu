package com.jinshu.common.security;

import com.jinshu.common.config.EncryptionProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyManagerTest {

    private static final String KEY_V1 = "Wi3DdD12Qg1KkvPcS530swxnrzSWCnQN0x3OPCJk8zU=";
    private static final String KEY_V2 = "kMrXTbivAIfNiHwo6WY2npQ8OKlOZYjEIx/gOX/de6w=";

    private KeyManager createKeyManager(Map<Integer, String> keys, Integer latestVersion) {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setKeys(keys);
        properties.setLatestVersion(latestVersion);
        KeyManager keyManager = new KeyManager(properties);
        keyManager.init();
        return keyManager;
    }

    @Test
    void shouldEncryptAndDecryptWithLatestVersion() {
        KeyManager keyManager = createKeyManager(Map.of(1, KEY_V1), 1);
        String plaintext = "hello jinshu";
        String encrypted = keyManager.encrypt(plaintext);

        assertTrue(encrypted.startsWith("1:"));
        assertEquals(plaintext, keyManager.decrypt(encrypted));
    }

    @Test
    void shouldDecryptOldVersionWithMultipleKeys() {
        KeyManager keyManager = createKeyManager(Map.of(1, KEY_V1, 2, KEY_V2), 2);

        // 用 v1 密钥加密一段旧密文
        String oldEncrypted = CryptoUtils.encrypt(Base64.getDecoder().decode(KEY_V1), 1, "old data");
        String newEncrypted = keyManager.encrypt("new data");

        assertTrue(newEncrypted.startsWith("2:"));
        assertEquals("old data", keyManager.decrypt(oldEncrypted));
        assertEquals("new data", keyManager.decrypt(newEncrypted));
    }

    @Test
    void shouldAutoDetectLatestVersionWhenNotConfigured() {
        KeyManager keyManager = createKeyManager(Map.of(1, KEY_V1, 2, KEY_V2), null);
        assertEquals(2, keyManager.getLatestVersion());
    }

    @Test
    void shouldThrowWhenKeyMissing() {
        KeyManager keyManager = createKeyManager(Map.of(2, KEY_V2), 2);
        String oldEncrypted = CryptoUtils.encrypt(Base64.getDecoder().decode(KEY_V1), 1, "old data");

        SecurityException exception = assertThrows(SecurityException.class,
                () -> keyManager.decrypt(oldEncrypted));
        assertTrue(exception.getMessage().contains("未知"));
    }

    @Test
    void shouldThrowWhenNoKeysConfigured() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setKeys(Map.of());
        assertThrows(IllegalStateException.class, () -> new KeyManager(properties).init());
    }

    @Test
    void shouldDetectEncryptedFormat() {
        KeyManager keyManager = createKeyManager(Map.of(1, KEY_V1), 1);
        String encrypted = keyManager.encrypt("secret");

        assertTrue(keyManager.isEncrypted(encrypted));
        assertFalse(keyManager.isEncrypted("plaintext"));
        assertFalse(keyManager.isEncrypted(null));
    }
}
