package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Excel 模板文件实体
 *
 * 用于模板驱动导出：租户上传 .xlsx 模板并可选关联到具体报表。
 */
@Data
public class ExcelTemplateFile {

    /**
     * 模板文件 ID，主键
     */
    private Long id;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 关联报表 ID，可选
     */
    private Long reportId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件在存储系统中的绝对路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 创建人 ID
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
