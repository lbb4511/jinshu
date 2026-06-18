package com.jinshu.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 字段加密密钥配置属性
 *
 * 配置示例：
 * <pre>
 * jinshu:
 *   encryption:
 *     keys:
 *       1: base64EncodedKeyV1
 *       2: base64EncodedKeyV2
 *     latest-version: 2
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jinshu.encryption")
public class EncryptionProperties {

    /**
     * 版本号 → Base64 编码的 32 字节密钥
     */
    private Map<Integer, String> keys = new HashMap<>();

    /**
     * 当前最新密钥版本，用于新数据加密。
     * 未配置时自动取 keys 中最大版本号。
     */
    private Integer latestVersion;
}
