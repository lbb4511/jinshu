package com.jinshu.common.security;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 字段级加密工具类
 *
 * 设计要点：
 * - 采用 AES/GCM/NoPadding，提供认证加密（AEAD），可检测密文篡改
 * - 每次加密生成随机 12 字节 IV（Nonce）
 * - 密文存储格式：{keyVersion}:{base64(iv)}:{base64(ciphertext+authTag)}
 *
 * 注意：密钥必须为 256 bit（32 字节）。
 */
public final class CryptoUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;      // 96 bit，GCM 推荐长度
    private static final int GCM_TAG_LENGTH = 128;    // bit
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtils() {
    }

    /**
     * AES-256-GCM 加密
     *
     * @param key        32 字节密钥
     * @param keyVersion 密钥版本号，会写入密文前缀用于解密时选择密钥
     * @param plaintext  明文
     * @return 密文字符串，格式：{version}:{base64(iv)}:{base64(ciphertext)}
     */
    public static String encrypt(byte[] key, int keyVersion, String plaintext) {
        if (key == null || key.length != 32) {
            throw new SecurityException("AES-256 密钥必须为 32 字节");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return keyVersion + ":"
                    + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new SecurityException("AES-GCM 加密失败", e);
        }
    }

    /**
     * AES-256-GCM 解密
     *
     * @param key           32 字节密钥
     * @param encryptedText 密文字符串，格式：{version}:{base64(iv)}:{base64(ciphertext)}
     * @return 明文
     */
    public static String decrypt(byte[] key, String encryptedText) {
        if (key == null || key.length != 32) {
            throw new SecurityException("AES-256 密钥必须为 32 字节");
        }
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        try {
            String[] parts = encryptedText.split(":", 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("密文格式错误，期望 {version}:{base64(iv)}:{base64(ciphertext)}");
            }

            int version = Integer.parseInt(parts[0]);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] plainBytes = cipher.doFinal(ciphertext);
            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (AEADBadTagException e) {
            throw new SecurityException("密文完整性校验失败（可能被篡改）", e);
        } catch (NumberFormatException e) {
            throw new SecurityException("密文版本号解析失败", e);
        } catch (IllegalArgumentException e) {
            throw new SecurityException("密文解码失败", e);
        } catch (Exception e) {
            throw new SecurityException("AES-GCM 解密失败", e);
        }
    }
}
