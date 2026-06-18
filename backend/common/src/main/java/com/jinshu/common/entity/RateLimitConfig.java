package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 限流规则配置实体
 *
 * 支持多维度限流：
 * - USER：单用户限流
 * - TENANT：单租户限流
 * - LOGIN_IP：登录接口单 IP 限流
 *
 * 当 tenant_id 为 NULL 时表示全局默认规则，可被租户级规则覆盖。
 */
@Data
public class RateLimitConfig {

    /**
     * 规则 ID
     */
    private Long id;

    /**
     * 租户 ID，NULL 表示全局默认规则
     */
    private Long tenantId;

    /**
     * 限流维度：USER / TENANT / LOGIN_IP
     */
    private String scope;

    /**
     * 资源匹配模式，默认 * 表示通配
     */
    private String resourcePattern;

    /**
     * 滑动窗口内允许的最大请求数
     */
    private Integer maxRequests;

    /**
     * 滑动窗口时长（秒）
     */
    private Integer windowSeconds;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
