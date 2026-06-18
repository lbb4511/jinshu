package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色变更审计日志实体
 *
 * 记录用户角色变更历史，包括目标用户、操作人、变更前后角色及原因。
 */
@Data
public class RoleChangeLog {

    /**
     * 日志ID，主键
     */
    private Long id;

    /**
     * 租户ID，多租户隔离
     */
    private Long tenantId;

    /**
     * 目标用户ID
     */
    private Long targetUserId;

    /**
     * 操作人用户ID
     */
    private Long operatorUserId;

    /**
     * 变更前角色
     */
    private String oldRole;

    /**
     * 变更后角色
     */
    private String newRole;

    /**
     * 变更原因
     */
    private String reason;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
