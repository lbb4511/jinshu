package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表实体类
 *
 * 报表元数据管理，不包含具体数据
 *
 * 功能说明：
 * - 报表基本信息管理
 * - 关联报表数据宽表
 * - 工作流状态跟踪
 */
@Data
public class Report {

    /**
     * 报表ID，主键
     */
    private Long id;

    /**
     * 租户ID，多租户隔离
     */
    private Long tenantId;

    /**
     * 报表名称
     */
    private String name;

    /**
     * 报表描述
     */
    private String description;

    /**
     * 表结构版本号
     */
    private Integer schemaVersion;

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    /**
     * 模板配置
     */
    private String templateConfig;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除标记
     */
    private Boolean isDeleted;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;
}
