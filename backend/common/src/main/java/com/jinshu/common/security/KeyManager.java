package com.jinshu.common.security;

import com.jinshu.common.config.EncryptionProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段级加密密钥管理器
 *
 * 职责：
 * - 管理多版本 AES-256 密钥
 * - 根据 latestVersion 加密新数据
 * - 根据密文前缀中的版本号选择对应密钥解密
 * - 提供加解密健康自检能力
 *
 * 密钥来源：
 * - 生产环境通过 K8s Secret / 环境变量注入
 * - 开发环境通过 application.yml 配置（仅用于开发，禁止生产使用）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeyManager {

    private final EncryptionProperties encryptionProperties;

    private final Map<Integer, byte[]> keys = new ConcurrentHashMap<>();
    private int latestVersion;

    @PostConstruct
    public void init() {
        Map<Integer, String> configuredKeys = encryptionProperties.getKeys();
        if (configuredKeys == null || configuredKeys.isEmpty()) {
            throw new IllegalStateException("未配置加密密钥（jinshu.encryption.keys），应用无法启动");
        }

        for (Map.Entry<Integer, String> entry : configuredKeys.entrySet()) {
            addKey(entry.getKey(), entry.getValue());
        }

        Integer configuredLatest = encryptionProperties.getLatestVersion();
        if (configuredLatest != null) {
            if (!keys.containsKey(configuredLatest)) {
                throw new IllegalStateException("配置的 latest-version " + configuredLatest + " 在 keys 中不存在");
            }
            this.latestVersion = configuredLatest;
        } else {
            this.latestVersion = keys.keySet().stream()
                    .max(Comparator.naturalOrder())
                    .orElseThrow(() -> new IllegalStateException("无法确定最新密钥版本"));
        }

        log.info("KeyManager 初始化完成，已加载 {} 个密钥版本，当前最新版本 v{}", keys.size(), latestVersion);

        // 启动时加解密自检
        healthCheck();
    }

    private void addKey(int version, String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("密钥版本 v" + version + " 未设置或为空");
        }
        byte[] decoded = Base64.getDecoder().decode(base64Key.trim());
        if (decoded.length != 32) {
            throw new IllegalStateException("密钥版本 v" + version + " 长度错误，期望 32 字节（Base64 编码后 44 字符），实际 " + decoded.length + " 字节");
        }
        keys.put(version, decoded);
    }

    /**
     * 使用最新版本密钥加密明文
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        byte[] key = keys.get(latestVersion);
        return CryptoUtils.encrypt(key, latestVersion, plaintext);
    }

    /**
     * 根据密文前缀版本号解密密文
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        int version = extractVersion(encryptedText);
        byte[] key = keys.get(version);
        if (key == null) {
            throw new SecurityException("未知或已废弃的加密密钥版本: " + version);
        }
        return CryptoUtils.decrypt(key, encryptedText);
    }

    /**
     * 判断一段文本是否为当前 KeyManager 可识别的密文格式
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            int version = extractVersion(text);
            return version > 0 && text.split(":", 3).length == 3;
        } catch (Exception e) {
            return false;
        }
    }

    private int extractVersion(String encryptedText) {
        int colonIndex = encryptedText.indexOf(':');
        if (colonIndex <= 0) {
            throw new IllegalArgumentException("密文缺少版本号前缀");
        }
        return Integer.parseInt(encryptedText.substring(0, colonIndex));
    }

    /**
     * 加解密健康自检
     */
    public void healthCheck() {
        String test = "jinshu_health_check_" + System.currentTimeMillis();
        String encrypted = encrypt(test);
        String decrypted = decrypt(encrypted);
        if (!test.equals(decrypted)) {
            throw new IllegalStateException("KeyManager 加解密自检失败");
        }
    }

    public int getLatestVersion() {
        return latestVersion;
    }
}
