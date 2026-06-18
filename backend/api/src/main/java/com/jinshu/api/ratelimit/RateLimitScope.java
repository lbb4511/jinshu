package com.jinshu.api.ratelimit;

/**
 * 限流维度枚举
 */
public enum RateLimitScope {
    /**
     * 用户级限流
     */
    USER,
    /**
     * 租户级限流
     */
    TENANT,
    /**
     * 登录接口 IP 级限流
     */
    LOGIN_IP
}
