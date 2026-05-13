package com.jinshu.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 哈希工具类
 *
 * 提供SHA-256等哈希算法
 *
 * 使用场景：
 * - 审计日志哈希链防篡改
 * - 文件完整性校验
 * - 密码哈希（注意：密码建议使用BCrypt）
 *
 * 注意事项：
 * - SHA-256用于完整性校验，不建议直接用于密码存储
 * - 密码存储应该使用BCrypt/Argon2等慢哈希算法
 */
public class HashUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private HashUtils() {
    }

    /**
     * 计算SHA-256哈希值
     *
     * @param input 输入字符串
     * @return 64位十六进制哈希字符串
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
