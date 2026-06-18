package com.jinshu.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API 限流配置属性
 */
@Data
@ConfigurationProperties(prefix = "jinshu.ratelimit")
public class RateLimitProperties {

    /**
     * 是否启用限流与租户并发配额拦截
     */
    private boolean enabled = true;

    /**
     * 登录接口路径，用于识别登录 IP 限流
     */
    private String loginPath = "/auth/login";

    /**
     * 用户级默认 QPS（当数据库未配置时生效）
     */
    private int defaultUserQps = 100;

    /**
     * 租户级默认 QPS（当数据库未配置时生效）
     */
    private int defaultTenantQps = 1000;

    /**
     * 登录接口默认单 IP 每分钟请求数（当数据库未配置时生效）
     */
    private int defaultLoginPerMinute = 10;

    /**
     * 租户默认最大并发请求数（当数据库未配置时生效）
     */
    private int defaultTenantConcurrency = 100;

    /**
     * 并发计数器 Redis 过期时间（秒），防止进程崩溃后计数器长期残留
     */
    private int concurrencyCounterTtlSeconds = 60;
}
