package com.jinshu.common.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoUtilsTest {

    private static final byte[] KEY = Base64.getDecoder().decode("Wi3DdD12Qg1KkvPcS530swxnrzSWCnQN0x3OPCJk8zU=");

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        String plaintext = "{\"password\":\"secret123\",\"sslEnabled\":true}";
        String encrypted = CryptoUtils.encrypt(KEY, 1, plaintext);

        assertNotEquals(plaintext, encrypted);
        assertTrue(encrypted.startsWith("1:"));
        assertEquals(3, encrypted.split(":").length);

        String decrypted = CryptoUtils.decrypt(KEY, encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void shouldProduceDifferentCiphertextForSamePlaintext() {
        String plaintext = "same text";
        String encrypted1 = CryptoUtils.encrypt(KEY, 1, plaintext);
        String encrypted2 = CryptoUtils.encrypt(KEY, 1, plaintext);

        assertNotEquals(encrypted1, encrypted2);
        assertEquals(plaintext, CryptoUtils.decrypt(KEY, encrypted1));
        assertEquals(plaintext, CryptoUtils.decrypt(KEY, encrypted2));
    }

    @Test
    void shouldThrowWhenDecryptingWithWrongKey() {
        byte[] wrongKey = Base64.getDecoder().decode("dGVzdEtleVRlc3RLZXlUZXN0S2V5VGVzdEtleVRlc3RLZXk=");
        String plaintext = "sensitive";
        String encrypted = CryptoUtils.encrypt(KEY, 1, plaintext);

        assertThrows(SecurityException.class, () -> CryptoUtils.decrypt(wrongKey, encrypted));
    }

    @Test
    void shouldThrowWhenCiphertextIsTampered() {
        String plaintext = "sensitive";
        String encrypted = CryptoUtils.encrypt(KEY, 1, plaintext);
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "abcd";

        SecurityException exception = assertThrows(SecurityException.class,
                () -> CryptoUtils.decrypt(KEY, tampered));
        assertTrue(exception.getMessage().contains("完整性校验失败") || exception.getMessage().contains("解密失败"));
    }

    @Test
    void shouldRejectInvalidKeyLength() {
        byte[] shortKey = new byte[16];
        assertThrows(SecurityException.class, () -> CryptoUtils.encrypt(shortKey, 1, "text"));
    }

    @Test
    void shouldHandleEmptyPlaintext() {
        String encrypted = CryptoUtils.encrypt(KEY, 1, "");
        assertFalse(encrypted.isEmpty());
        assertEquals("", CryptoUtils.decrypt(KEY, encrypted));
    }

    @Test
    void shouldHandleNullOnDecrypt() {
        assertNullWorks(null);
    }

    private void assertNullWorks(String value) {
        assertEquals(value, CryptoUtils.decrypt(KEY, value));
    }
}
