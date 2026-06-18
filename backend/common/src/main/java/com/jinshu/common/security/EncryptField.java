package com.jinshu.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段加密注解
 *
 * 用于标记需要经过 EncryptTypeHandler 自动加解密的字段。
 * 当前主要配合 MyBatis TypeHandler 使用，实现对数据库敏感字段的透明加解密。
 *
 * 使用示例：
 * <pre>
 * public class DataSource {
 *     &#064;EncryptField
 *     private String connectionConfig;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptField {

    /**
     * 加密密钥版本，默认 0 表示使用 KeyManager 当前最新版本
     */
    int keyVersion() default 0;

    /**
     * 是否启用加密。可临时关闭以支持灰度或回滚。
     */
    boolean enabled() default true;
}
