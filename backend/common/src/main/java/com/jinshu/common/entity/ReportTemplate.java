package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表模板实体类
 *
 * 用于模板市场：系统级公共模板 + 租户私有/公开模板
 */
@Data
public class ReportTemplate {

    /**
     * 模板ID，主键
     */
    private Long id;

    /**
     * 租户ID，0 表示系统级公共模板
     */
    private Long tenantId;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板分类，如 SALES、FINANCE、HR
     */
    private String category;

    /**
     * 缩略图 URL
     */
    private String thumbnailUrl;

    /**
     * 报表布局 JSON
     */
    private String layoutJson;

    /**
     * 示例数据 JSON
     */
    private String sampleData;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 是否系统级模板，系统级模板不可修改/删除
     */
    private Boolean isSystem;

    /**
     * 状态：ACTIVE/INACTIVE
     */
    private String status;

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
}
