package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户实体类
 *
 * 多租户系统的核心实体，代表一个独立的租户组织
 *
 * 功能说明：
 * - 租户基础信息管理
 * - 资源配额配置（存储、并发等）
 * - 租户状态控制（启用/禁用）
 */
@Data
public class Tenant {

    /**
     * 租户ID，主键
     */
    private Long id;

    /**
     * 租户名称
     */
    private String name;

    /**
     * 租户编码，唯一标识
     */
    private String code;

    /**
     * 租户状态：ACTIVE-启用，DISABLED-禁用
     */
    private String status;

    /**
     * 配额配置，JSON格式存储
     * 包含存储容量、并发任务数等配置
     */
    private String quotaConfig;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
