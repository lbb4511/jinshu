package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感字段脱敏规则实体
 *
 * 用于按角色对手机号、身份证、金额等敏感字段进行脱敏。
 */
@Data
public class DesensitizeRule {

    /**
     * 规则 ID
     */
    private Long id;

    /**
     * 租户 ID，0 表示全局规则
     */
    private Long tenantId;

    /**
     * 目标表名
     */
    private String tableName;

    /**
     * 目标列名
     */
    private String columnName;

    /**
     * 脱敏规则类型，如 MASK_PHONE、MASK_ID_CARD、MASK_AMOUNT 等
     */
    private String ruleType;

    /**
     * 适用角色列表，逗号分隔，如 USER,VIEWER
     */
    private String applicableRoles;

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
