package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户并发请求配额实体
 *
 * 用于限制单个租户在同一时刻能够处理的活跃请求数量，
 * 防止单个租户占用过多应用线程/连接资源。
 */
@Data
public class TenantConcurrencyQuota {

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 最大并发活跃请求数
     */
    private Integer maxConcurrent;

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
