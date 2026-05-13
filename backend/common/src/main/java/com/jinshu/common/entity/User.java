package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 系统用户信息，关联到具体租户
 *
 * 功能说明：
 * - 用户认证与授权
 * - 角色权限管理
 * - 审计日志关联
 */
@Data
public class User {

    /**
     * 用户ID，主键
     */
    private Long id;

    /**
     * 租户ID，多租户隔离
     */
    private Long tenantId;

    /**
     * 用户名，唯一
     */
    private String username;

    /**
     * 密码哈希（BCrypt加密）
     */
    private String passwordHash;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 用户角色：ADMIN-管理员，USER-普通用户，VIEWER-只读用户
     */
    private String role;

    /**
     * 用户状态：ACTIVE-启用，DISABLED-禁用
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
